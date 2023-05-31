package com.smartpos.demo.serial

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.aill.androidserialport.SerialPort
import com.smartpos.demo.IChannel
import com.smartpos.demo.utils.ChannelCallback
import com.smartpos.demo.utils.CommUtil
import com.smartpos.demo.utils.HexUtil
import com.smartpos.demo.utils.RetryWithDelay
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.experimental.xor

/**
 * 通道：usb
 *
 * 随有效数据发送ack：
 * 如果发送ack时正好有其他数据需要发送，可将数据包中ack字段赋值。
 *
 *
 * 单独发送ack：
 * 发送一个空包，ack字段非0，数据包id为0，有效数据长度为0。
 * 因为数据包id为0，接收方不会记录该值，也不会为这种包回传ack。
 *
 *
 * 握手成功：pc向pos发送一个空包，包类型为1，ack字段为0，数据包id为0。
 * pos端收到后将当前状态设为连接并向pos回一个包类型为2的空包。
 * 如果pos端在收到握手命令之前是在连接状态，则需要清空相关状态。
 *
 *
 * 定时发包：
 * 发包间隔为100毫秒。在本地维护一个待发送有效数据包的栈，设一个定时器，每50毫秒发送栈顶的数据包。
 * 如果栈是空的，但是有回传ack的需要，则发送一个带ack的空包。
 *
 *
 * 重发机制：
 * 栈顶的新的数据包，发送出去后，将其标记为已发送，并记录该包发送的次数为1。
 * 到下一个100毫秒的发包时间，在发包之前检查收包情况，如果收到对方的对应ack，将该数据包从栈移除。
 * 如果还没收到，则重发该包，将该包发送的次数+1。如果已发送5次仍没有收到对面的ack消息，将连接设为断开并清空状态。
 *
 *
 * 对接收到的包的处理：
 * 对于对方发来的带有效数据的包，接收方要及时回传ack。
 * 有可能会出现同时接收多个不同的数据包的情况（id不一样），这时只需要回传最后一个包的ack。
 * 为了防止重复处理对方重发的包，接收方要记录上一次接收到的数据包的id，如果id相等则代表是对方重发的，自动忽略该包。
 *
 * 心跳机制
 * pc与pos任何一方，只要收不到对方的合法包超过1秒钟，则认为连接断开。
 * 在没有业务处于空闲状态时，为了保持连接，需要发心跳包。
 * 心跳包两方都需要发送，发送条件为：距离上一次成功发包的时间间隔大于等于200毫秒。
 * 心跳包就是一个没有数据的空包，数据包id为0，有效数据长度为0。
 *
 */
class Usb2(val context: Context) : IChannel {


    private val isShowLog = true


    private val maxLength = 1024//定义一个包的最大长度，2个字节表示的最大长度
    private val starCodeLength = 2//起始符
    private val packageTypeLength = 1//包类型
    private val ackLength = 1//ack
    private val packageIdLength = 1//包id
    private val dataLength = 2//包长度
    private val headerLength =
        starCodeLength + packageTypeLength + ackLength + packageIdLength + dataLength//协议头长度，有效数据之前的长度
    private val checkCodeLength = 1//校验和长度1
    private val endCodeLength = 2//终止符
    private val head1 = HexUtil.hexToBytes("55")[0]//同步头1
    private val head2 = HexUtil.hexToBytes("AA")[0]//同步头2
    private val packageType_common = 0x00
    private val packageType_handshake = 0x01
    private val packageType_handshake_confirm = 0x02


    private val baudrate = 115200//波特率
    private var DEV_USB_FILE_PREFIX = "/dev/ttyGS" // 默认文件位置
    private var callback: ChannelCallback? = null
    private var serialPort: SerialPort? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var readThread: ReadThread? = null

    private var isWorking = false

    private var inNeedConnect = true
    private val msgQueue = ConcurrentLinkedQueue<CommonPackage>()//消息队列
    private var ackPackage: AckPackage? = null//待发送的ack包
    private var handshakeonfirmPackage: HandshakeConfirmPackage? = null//待发送的握手确认包
    private var msgId = 1 //消息id in 1..255
    private val SEND_MESSAGE = 1000
    private val SEND_HEARTBEAT = 1001
    private val READ_MESSAGE_INTERVAL = 50L //接受消息间隔
    private val SEND_MESSAGE_INTERVAL = 100L //发送消息间隔
    private val SEND_HEARTBEAR_INTERVAL = 2000L //发送心跳间隔
    private val DISCONNECT_INTERVAL = 6000L //超过间隔时间没有收到合法包则断开连接
    private val sentPackage = HashMap<Byte, Int>(1)//已经发送的数据包，包括Id，次数
    private var isConnected = false //连接状态，默认false
    private var lastReceivedMsgId = 0x00//上一次收到的信息的Id，如果Id相等则代表是对方重发的，自动忽略该包
    private var lastReceivedTime = 0L//上一次收到的合法包的时间
    private var lastReceivedHandShakeTime = 0L//上一次收到的握手包的时间
    private val simpleMode = true//简单模式：无心跳、连接检测

    private val mHandler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            SEND_MESSAGE -> {
                if (checkConnect()) {
                    sendMsg()
                    sendNextMsg()
                }
            }

            SEND_HEARTBEAT -> {
                msgQueue.add(HeartBeatPackage())
                sendNextHearBeat()
            }
        }
        false
    }

    private fun sendNextMsg() {
        if (isConnect()) {
            mHandler.sendEmptyMessageDelayed(SEND_MESSAGE, SEND_MESSAGE_INTERVAL)
        }
    }

    private fun sendNextMsgImmediately() {
        if (isConnect()) {
            mHandler.sendEmptyMessageDelayed(SEND_MESSAGE, 0)
        }
    }

    private fun sendNextHearBeat() {
        if (isConnect()) {
            mHandler.sendEmptyMessageDelayed(SEND_HEARTBEAT, SEND_HEARTBEAR_INTERVAL)
        }
    }

    /**
     * 只要收不到对方的合法包超过x秒钟，则认为连接断开
     */
    private fun checkConnect(): Boolean {
        if (simpleMode) {
            return true
        }
        return if (System.currentTimeMillis() - lastReceivedTime > DISCONNECT_INTERVAL) {
            Log.i("Usb", "超过" + DISCONNECT_INTERVAL + "ms没有收到合法包，断开连接")
            disConnect()
            false
        } else {
            true
        }
    }

    private fun setUsbMode(mode: Int) {
        try {
            val wangPosManagerClass = Class.forName("android.os.WangPosManager")
            val wangPosManager = wangPosManagerClass.newInstance()
            val method = wangPosManagerClass.getMethod("setUsbMode", Int::class.javaPrimitiveType)
            method.invoke(wangPosManager, mode)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        }
    }

    override fun connect() {
        if (!CommUtil.isUsbConnected(context)) {
            Log.i("Usb", "请插入连接线")
            // disConnect()
            callback?.onConnectFail("请插入连接线")
            return
        }
        if (!Build.MODEL.contains("WPOS")) {
            setUsbMode(1)
        } else {
            setUsbMode(0)
        }
//        if (isConnected) {
//            disConnect()
//        }
        //开始连接
        callback?.onConnectStart()

        Single.create<Boolean> {
            Log.i("Usb", "开始打开串口")

            var isOpenFile = false
            for (i in 0..10) {
                val fileName = DEV_USB_FILE_PREFIX + i
                if (File(fileName).exists()) {
                    isOpenFile = openSerialPort(fileName)
                    if (isOpenFile) {
                        break
                    } else {
                        println("打开文件失败:$fileName")
                    }
                } else {
                    println("文件不存在:$fileName")
                }
            }
            if (isOpenFile) {
                it.onSuccess(true)
            } else {
                it.onError(RuntimeException("打开串口失败"))
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .retryWhen(RetryWithDelay(3, 1000))
            .subscribe(object : SingleObserver<Boolean> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onSuccess(isOpen: Boolean) {
                    if (isOpen) {
                        Log.i("Usb", "打开串口成功")
                        startRead()

                        if (simpleMode) {
                            callback?.onConnectSuccess(false)
                            //开始定时发包
                            isConnected = true
                            sendNextMsgImmediately()
                        }

                    } else {
                        Log.i("Usb", "打开串口失败")
                    }
                }

                override fun onError(e: Throwable) {
                    Log.i("Usb", "打开串口失败，请重试")
                    callback?.onConnectFail("打开串口失败，请重试")
                }

            })
    }

    override fun disConnect() {
        close()
        clearStatus()
    }

    override fun isConnect(): Boolean {
        return isConnected
    }

    override fun sendMsg(msg: String) {
        val pack = MsgPackage(msg.toByteArray())
        msgQueue.add(pack)
    }

    override fun setChannelCallback(channelCallback: ChannelCallback) {
        callback = channelCallback
    }

    private fun getSerialPort(fileName: String): SerialPort? {
        val file = File(fileName)
        var sp: SerialPort? = null

        try {
            sp = SerialPort(file, baudrate, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("Usb", "打开串口失败：" + e.message)
        }

        return sp
    }

    private fun openSerialPort(fileName: String): Boolean {
        println("openSerialPort: $fileName")
        serialPort = getSerialPort(fileName)
        if (serialPort == null) {
            return false
        }
        outputStream = serialPort?.outputStream
        inputStream = serialPort?.inputStream
        return true
    }

    private fun startRead() {
        readThread = ReadThread()
        isWorking = true
        readThread?.start()
    }

    private fun close() {
        inNeedConnect = false
        Log.e("Usb", "关闭 isWorking：$isWorking")
        if (isWorking) {
            isWorking = false
            showLog("close isWorking false")
            readThread?.interrupt()
            readThread = null
        }

        if (serialPort != null) {
            try {
                serialPort?.close()
                outputStream?.close()
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                serialPort = null
                outputStream = null
                inputStream = null
            }
        }

        if (isConnected) {
            //连接断开回调
            callback?.onDisconnect()
            isConnected = false
        } else {
            callback?.onConnectFail("连接失败")
        }
    }

    private inner class ReadThread : Thread() {
        override fun run() {
            super.run()
            val buffer = ByteArray(maxLength)//缓冲区
            var available = 0//缓冲区剩余空间
            var currentLength = 0 //当前已收到包的总长度
            var read = 0

            while (isWorking) {
                val time = System.currentTimeMillis()
                //防止超出缓冲区最大长度溢出
                available = maxLength - currentLength
                try {
                    read = inputStream?.read(buffer, currentLength, available)!!
                } catch (e: Exception) {
                    showLog("读取数据异常关闭连接")
                    if (isWorking) {
                        disConnect()
                    }
                }
                if (read > 0) {
                    val handler = Handler(Looper.getMainLooper())
                    showLog("读取数据长度：" + read + "，时间：" + System.currentTimeMillis())
                    currentLength += read
                } else {
                    continue
                }
                //指针移动到缓冲区头部
                var cursor = 0
                //如果当前收到包大于头长度，则解析当前包
                while (currentLength >= headerLength) {
                    showLog("当前包长度大于头长度，解析当前包")
                    showLog("currentLength=$currentLength cursor=$cursor")
                    showLog(
                        "当前包内容：" + HexUtil.byte2hex(
                            getCurrentArray(
                                buffer,
                                cursor,
                                currentLength
                            )
                        )
                    )
                    //取到头部起始符，起始符之前的数据将被丢弃
                    if (!(buffer[cursor] == head1 && buffer[cursor + 1] == head2)) {
                        showLog("未取到头部起始符，包长度-1，指针后移1位")
                        --currentLength
                        ++cursor
                        continue
                    }
                    val len = starCodeLength + packageTypeLength + ackLength + packageIdLength
                    val contentLength = parseLen(buffer[cursor + len], buffer[cursor + len + 1])
                    //如果内如包的长度大于最大内容长度，则说明这个包有问题，丢弃
                    if (contentLength > maxLength - headerLength - checkCodeLength - endCodeLength) {
                        currentLength = 0
                        println("这个包有问题，丢弃")
                        break
                    }

                    //如果当前获取到长度小于整个包的长度，则跳出循环继续接收数据
                    val factPackLength =
                        contentLength + headerLength + checkCodeLength + endCodeLength
                    if (currentLength < factPackLength) {
                        showLog("当前获取到长度小于整个包的长度，跳出循环继续接收数据")
                        break
                    }
                    //完整包产生
                    val pack = ByteArray(factPackLength)
                    System.arraycopy(buffer, cursor, pack, 0, factPackLength)
                    onDataReceived(pack)
                    //指针移动到整包的尾部，长度为剩余数据长度
                    currentLength -= factPackLength
                    cursor += factPackLength
                }

                //残留字节移到缓冲区首
                if (currentLength > 0 && cursor > 0) {
                    showLog("残留字节移到缓冲区首")
                    showLog("currentLength=$currentLength cursor=$cursor")
                    showLog(
                        "移动内容=" + HexUtil.byte2hex(
                            getCurrentArray(
                                buffer,
                                cursor,
                                currentLength
                            )
                        )
                    )
                    System.arraycopy(buffer, cursor, buffer, 0, currentLength)
                }
                //使用while循环模拟sleep
                //50ms读取一次
                while (System.currentTimeMillis() - time < READ_MESSAGE_INTERVAL) {
                }
            }
        }
    }

    private fun getCurrentArray(buffer: ByteArray, cursor: Int, currentLength: Int): ByteArray {
        val buf = ByteArray(currentLength)
        System.arraycopy(buffer, cursor, buf, 0, currentLength)
        return buf
    }

    private fun getLen(pack: ByteArray): ByteArray {
        val dataLen = ByteArray(dataLength)
        System.arraycopy(
            pack,
            starCodeLength + packageTypeLength + ackLength + packageIdLength,
            dataLen,
            0,
            dataLength
        )
        return dataLen
    }


    private fun onDataReceived(pack: ByteArray) {
        println("收到信息：" + HexUtil.byte2hex(pack))
        //获取需要校验的信息
        val checkDataLen = pack.size - starCodeLength - checkCodeLength - endCodeLength
        val checkCodeBuffer = ByteArray(checkDataLen)
        System.arraycopy(pack, starCodeLength, checkCodeBuffer, 0, checkDataLen)
        //校验和
        val checkCode = pack[pack.size - 1 - endCodeLength]
        //校验包没有问题
        if (getCheckCode(checkCodeBuffer) == checkCode) {
            //记录收到合法包的时间
            lastReceivedTime = System.currentTimeMillis()
            //消息类型
            val packageType = pack[starCodeLength]
            //ack
            val ack = pack[starCodeLength + packageTypeLength]
            //消息id
            val id = pack[starCodeLength + packageTypeLength + ackLength]
            //获取内容长度
            val dataLen = getLen(pack)
            val contentLen = parseLen(dataLen[0], dataLen[1])
            //获取内容信息
            val contentBuffer = ByteArray(contentLen)
            if (contentLen > 0) {
                System.arraycopy(pack, headerLength, contentBuffer, 0, contentLen)
            }
            when (packageType.toInt()) {
                packageType_common -> {//正常消息
                    //判断是否包含ack,如果包含则移除栈顶已发送的数据包
                    if (ack.toInt() != 0x00) {
                        removePackage(ack)
                    }
                    //非空包,即包含有效数据的包
                    if (id.toInt() != 0x00) {
                        //发送校验成功ACK
                        ackPackage = AckPackage(id)
                        //检查消息Id，如果与上次消息相同则忽略
                        if (id.toInt() == lastReceivedMsgId) {
                            println("消息ID相同忽略，" + HexUtil.byte2hex(id))
                            return
                        }
                        //设置最后一次接收到的信息id
                        lastReceivedMsgId = id.toInt()
                        //显示信息
                        showMsg(contentBuffer)
                        val commonPackage = CommonPackage(
                            packageType = packageType,
                            ack = ack,
                            id = id,
                            dataLength = dataLen,
                            data = contentBuffer,
                            checkCode = checkCode
                        )
                        showLog(commonPackage.toString())
                    } else {
                        //空包，目前为心跳包
                        if (!isConnected) {
                            Log.i(
                                "Usb",
                                "未连接状态, 收到心跳包, 说明app端重连后服务端还没有断开还在发送心跳"
                            )
                            isConnected = true

                            //开始定时发包
                            mHandler.removeMessages(SEND_MESSAGE)
                            mHandler.removeMessages(SEND_HEARTBEAT)
                            sendNextMsgImmediately()
                            sendNextHearBeat()

                            //握手之后调用连接成功回调
                            callback?.onConnectSuccess(true)
                        }
                    }
                }

                packageType_handshake -> {//握手命令
                    //pc发起重连会1秒钟发一次握手，pos没有接收数据时，信息放置于缓冲区
                    //如果1秒钟之内收到握手包就忽略
                    if ((System.currentTimeMillis() - lastReceivedHandShakeTime) < 1000) {
                        lastReceivedHandShakeTime = System.currentTimeMillis()
                        println("握手消息频繁忽略")
                        return
                    }
                    lastReceivedHandShakeTime = System.currentTimeMillis()
                    //如果之前是连接状态，则清空相关状态
                    if (isConnect()) {
                        clearStatus()
                    }
                    //将状态设为连接
                    //并发送握手确认包
                    isConnected = true
                    handshakeonfirmPackage = HandshakeConfirmPackage()
                    //pc端收到握手确认之后才开始发送心跳，所以记录收到合法包的时间延长2秒
                    lastReceivedTime = System.currentTimeMillis() + 2000
                    //开始定时发包
                    mHandler.removeMessages(SEND_MESSAGE)
                    mHandler.removeMessages(SEND_HEARTBEAT)
                    sendNextMsgImmediately()
                    sendNextHearBeat()
                    //握手之后调用连接成功回调
                    callback?.onConnectSuccess(false)
                }

                else -> {//不支持的消息类型
                    println("不支持的消息类型")
                }
            }
        } else {//校验包有问题不做处理
            println("校验包有问题不做处理")
        }
    }

    private fun clearStatus() {
        mHandler.removeMessages(SEND_MESSAGE)
        mHandler.removeMessages(SEND_HEARTBEAT)
        msgQueue.clear()//清空消息队列
        ackPackage = null//清空待发送的ack包
        handshakeonfirmPackage = null//清空待发送的握手确认包
        msgId = 0 //重置发送消息id
        sentPackage.clear()//清空已经发送的数据包
        lastReceivedMsgId = 0x00//重置上一次收到的信息的Id
        lastReceivedHandShakeTime = 0//重置收到握手包时间
        lastReceivedTime = 0//重置收到合法包时间
    }


    private fun removePackage(ack: Byte) {
        println("收到ack:" + HexUtil.byte2hex(ack) + ",移除消息")
        //已发送数据包及次数清空
        sentPackage.clear()
        //把次消息从消息队列中移除
        if (msgQueue.size > 0 && msgQueue.peek().id == ack) {
            msgQueue.poll()
            println("移除消息成功")
        } else {
            println("移除消息失败 size: " + msgQueue.size + " id: " + if (msgQueue.size > 0) msgQueue.peek().id else null)
        }
    }

    /**
     * 向业务层发送数据
     */
    private fun showMsg(contentBuffer: ByteArray) {
        var msg = String(contentBuffer, Charset.forName("UTF-8"))
        callback?.readMsg(msg)
        showLog(msg)
    }

    private fun sendSerialData(content: ByteArray): Boolean {
        return if (outputStream != null) {
            try {
                outputStream?.write(content)
                showLog("发送数据时间：" + System.currentTimeMillis())
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        } else {
            false
        }
    }


    /**
     * Int 类型转2字节长度
     */
    private fun getLen(length: Int): ByteArray {
        val len = ByteArray(2)
        len[0] = (length shr 8 and 0xFF).toByte()
        len[1] = (length shr 0 and 0xFF).toByte()
        return len
    }

    /**
     * 校验和
     */
    private fun getCheckCode(datas: ByteArray): Byte {
        var temp = datas[0]
        for (i in 1 until datas.size) {
            temp = temp xor datas[i]
        }
        return temp
    }

    /**
     * 获取协议内容长度
     *
     * @return
     */
    private fun parseLen(a: Byte, b: Byte): Int {
        var rlt = a.toInt() and 0xFF shl 8
        rlt += b.toInt() and 0xFF shl 0
        return rlt
    }


    private fun sendMsg() {
        var commonPackage: CommonPackage? = null
        //由于心跳包是没200ms发送一个，为了去除冗余包
        //如果队列里数据大于1条，并且第一条是心跳包则移除
        while (msgQueue.size > 1 && msgQueue.peek() is HeartBeatPackage) {
            msgQueue.poll()
        }
        if (msgQueue.size > 0) {
            commonPackage = msgQueue.peek()
        }
        var data: ByteArray? = null
        when {
            //如果握手确认包不为null则发送握手确认包，否则发送正常包
            handshakeonfirmPackage != null -> {
                data = handshakeonfirmPackage!!.getBytes()
                //发送后删除
                handshakeonfirmPackage = null
            }

            commonPackage != null -> {
                when (//心跳包
                    commonPackage) {
                    is HeartBeatPackage -> {
                        data = commonPackage.getBytes()
                        //发送后删除
                        msgQueue.poll()
                    }
                    //信息包
                    is MsgPackage -> {
                        //检测信息包的id，如果为null则赋值
                        if (commonPackage.id == null) {
                            commonPackage.id = getMsgId()
                        }
                        //检测发送次数
                        //                        checkTimes(commonPackage.id!!)
                        //如果同时存在信息包和ack包要发送，则合并两个包一起发送
                        //如果信息包中已经包含ack则覆盖ack，即只发送最后一个ack
                        if (ackPackage != null) {
                            commonPackage = mergePackage(commonPackage, ackPackage!!)
                            //发送后删除
                            ackPackage = null
                        }
                        data = commonPackage.getBytes()
                    }
                }
            }
            //ack包
            //由于ack包没有确认，发送后无论成功与否都删除
            ackPackage != null -> {
                data = ackPackage!!.getBytes()
                //发送后删除
                ackPackage = null
            }
        }
        data?.let {
            sendSerialData(data!!)
        }
    }

    /**
     * msgId in 1..255
     */
    private fun getMsgId(): Byte {
        if (++msgId > 255) {
            msgId = 1
        }
        return msgId.toByte()
    }

    private fun mergePackage(commonPackage: CommonPackage, ackPackage: AckPackage): CommonPackage {
        commonPackage.ack = ackPackage.ack
        return commonPackage
    }


    private fun showLog(msg: String) {
        if (isShowLog) {
            println(msg)
        }
    }


    /**
     * 数据包解构：
     *
     * 同步头：         2字节
     * 0x55 0xAA
     *
     *
     * 包类型：         1字节
     * 0x00：普通包；
     * 0x01：握手命令；
     * 0x02：握手确认命令；
     *
     *
     * ack字段：        1字节
     * 如果该字段为0，代表该字段无效；
     * 如果非0，代表最后一次接收到的、通过校验的数据包的id。
     *
     *
     * 数据包id：       1字节
     * 如果包中不带有效数据，该值为0。
     * 这个数据包id需要在本地维护，初始值可以为1，每次发新的数据包自动+1。
     * 加到255时，再往上加就变为1。
     *
     *
     * 有效数据长度：   2字节
     * 高位在前
     *
     *
     * 有效数据：       长度由上一个字段决定
     *
     *
     * 校验和：         1字节
     * 从同步头到有效数据所有字节的异或值
     *
     *
     * acK空包：ack字段非0，数据包id为0，有效数据长度为0
     *
     */
    open inner class CommonPackage(
        val head: ByteArray = HexUtil.hexToBytes("55AA"),
        var packageType: Byte?,
        open var ack: Byte?,
        var id: Byte?,
        val dataLength: ByteArray,
        open var data: ByteArray?,
        var checkCode: Byte?,
        val end: ByteArray = HexUtil.hexToBytes("CC33")
    ) {

        open fun getBytes(): ByteArray {
            val dataLen = data?.size ?: 0
            val lengthCheck = headerLength - starCodeLength + dataLen
            val bufferCheck = ByteBuffer.allocate(lengthCheck)
            bufferCheck.put(packageType!!)
            bufferCheck.put(ack!!)
            bufferCheck.put(id!!)
            bufferCheck.put(dataLength)
            if (dataLen > 0) {
                bufferCheck.put(data)
            }
            checkCode = getCheckCode(bufferCheck.array())
            val buffer =
                ByteBuffer.allocate(starCodeLength + lengthCheck + checkCodeLength + endCodeLength)
            buffer.put(head)
            buffer.put(bufferCheck.array())
            buffer.put(checkCode!!)
            buffer.put(end)
            val content = buffer.array()
            println("发送数据：" + HexUtil.byte2hex(content))
            showLog(toString())
            return content
        }

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("起始符：")
            sb.append(HexUtil.byte2hex(head))
            sb.append("\n")
            sb.append("包类型：")
            sb.append(HexUtil.byte2hex(packageType!!))
            sb.append("\n")
            sb.append("ack:")
            sb.append(HexUtil.byte2hex(ack!!))
            sb.append("\n")

            sb.append("id:")
            sb.append(HexUtil.byte2hex(id!!))
            sb.append("\n")
            sb.append("数据长度:")
            val dataLength = data?.size ?: 0
            sb.append(dataLength)
            sb.append("\n")
            if (dataLength > 0) {
                sb.append("数据内容:")
                sb.append(String(data!!, Charset.defaultCharset()))
                sb.append("\n")
            }
            sb.append("校验和:")
            sb.append(HexUtil.byte2hex(checkCode!!))
            sb.append("\n")
            sb.append("终止符:")
            sb.append(HexUtil.byte2hex(end))
            sb.append("\n")
            return sb.toString()
        }
    }


    /**
     * 普通包
     */
    inner class MsgPackage(override var data: ByteArray?) : CommonPackage(
        packageType = 0x00, ack = 0x00, id = null, dataLength = getLen(
            data?.size
                ?: 0
        ), data = data, checkCode = null
    )

    /**
     * acK包：ack字段非0，数据包id为0，有效数据长度为0
     */
    inner class AckPackage(override var ack: Byte?) : CommonPackage(
        packageType = 0x00,
        ack = ack,
        id = 0x00,
        dataLength = getLen(0),
        data = null,
        checkCode = null
    )

    /**
     * 握手确认包
     */
    inner class HandshakeConfirmPackage : CommonPackage(
        packageType = 0x02,
        ack = 0x00,
        id = 0x00,
        dataLength = getLen(0),
        data = null,
        checkCode = null
    )

    /**
     * 握手包
     */
    inner class HandshakePackage : CommonPackage(
        packageType = 0x01,
        ack = 0x00,
        id = 0x00,
        dataLength = getLen(0),
        data = null,
        checkCode = null
    )

    /**
     * 心跳包
     */
    inner class HeartBeatPackage : CommonPackage(
        packageType = 0x00,
        ack = 0x00,
        id = 0x00,
        dataLength = getLen(0),
        data = null,
        checkCode = null
    )
}