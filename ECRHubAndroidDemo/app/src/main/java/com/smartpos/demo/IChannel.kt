package com.smartpos.demo

import com.smartpos.demo.utils.ChannelCallback

/**
 * Created by wangpos on 17/10/31.
 */
interface IChannel {

    fun connect()

    fun disConnect()

    fun isConnect(): Boolean

    fun sendMsg(msg: String)

    fun setChannelCallback(channelCallback: ChannelCallback)


}