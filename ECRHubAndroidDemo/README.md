## 测试流程
详细的集成文档请参考[集成文档](https://gw.paycloud.world/docs/#/wisecashier/crossTerminalIntegrationUsb)

### 步骤

1. 在PC上安装高通驱动，驱动详见[qud.win.1.1_installer_10065.1.zip](https://github.com/paycloud-open/cahsier-ecr-hub/blob/main/usb-test-app/qud.win.1.1_installer_10065.1.zip)

2. 确保Wiseasy POS机器安装最新版本的Android系统

3. 准备一根USB数据线，将POS机与PC电脑连接

4. 安装PC上测试DEMO，[WiseConnecor](https://github.com/paycloud-open/cahsier-ecr-hub/blob/main/usb-test-app/WiseConnector-windows-20200221.msi)

5. POS上运行此DEMO，点击初始化按钮

6. 打开PC电脑的设备管理器，在端口中找到，名字中包含GPS的端口

7.打开WiseConnector程序，选择GPS对应的端口号，点击打开按钮。

8.点击此Demo程序的连接按钮，连接串口，连接成功之后，就可以通过WiseConnector，发送交易数据到POS了。

9. 在DEMO中的收到消息回调接口里面就能收到PC端发过来的数据了。

10. 收到数据之后就能通过DEMO中提供的发送数据接口将数据返回给PC了。. 