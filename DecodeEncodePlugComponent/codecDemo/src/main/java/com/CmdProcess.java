package com;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Base64;

public class CmdProcess {

    private String identifier = "123";
    private String msgType = "deviceReq";
    private String serviceId = "Brightness";
    private String cmd = "SET_POWER_MODE";
    private int hasMore = 0;
    private int errcode = 0;
    private int mid = 0;
    private JsonNode paras;

    public CmdProcess() {
    }

    public CmdProcess(ObjectNode input) {

        try {
            this.identifier = input.get("identifier").asText();
            this.msgType = input.get("msgType").asText();
            /*
            平台收到设备上报消息，编码ACK
            {
                "identifier":"0",
                "msgType":"cloudRsp",
                "request": ***,//设备上报的码流
                "errcode":0,
                "hasMore":0
            }
            */
            if (msgType.equals("cloudRsp")) {
                //在此组装ACK的值
                this.errcode = input.get("errcode").asInt();
                this.hasMore = input.get("hasMore").asInt();
            }

            /*
            平台下发命令到设备，输入
            {
                "identifier":0,
                "msgType":"cloudReq",
                "serviceId":"WaterMeter",
                "cmd":"SET_DEVICE_LEVEL",
                "paras":{"value":"20"},
                "hasMore":0

            }
            */
            else if (msgType.equals("cloudReq")) {
                //此处需要考虑兼容性，如果没有传mId，则不对其进行编码
                if (input.get("mid") != null) {
                    this.mid = input.get("mid").intValue();
                }
                this.cmd = input.get("cmd").asText();
                this.paras = input.get("paras");
                this.hasMore = input.get("hasMore").asInt();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public byte[] toByte() {
        try {
            if (this.msgType.equals("cloudReq")) {
                
                if (this.cmd.equals("SET_POWER_MODE")) {
                	this.serviceId="Configuration";
                    int status = paras.get("status").asInt();
                    //int timeout = paras.get("timeout").asInt();
                    byte[] byteRead = new byte[6];
                    byteRead[0] = (byte) 0xAA;
                    byteRead[1] = (byte) 0x71;
                    byteRead[2] = (byte) status;
                    byteRead[3] = (byte) 0xFF; //fix me 先固定写死
					//此处需要考虑兼容性，如果没有传mId，则不对其进行编码
                    if (Utilty.getInstance().isValidofMid(mid)) {
                        byte[] byteMid = new byte[2];
                        byteMid = Utilty.getInstance().int2Bytes(mid, 2);
                        byteRead[4] = byteMid[0];
                        byteRead[5] = byteMid[1];
                    }
                    return byteRead;
                }
                
                if (this.cmd.equals("SET_PRESSURE_READ_PERIOD")) {
                	this.serviceId="Illumination";
                    int value = paras.get("value").asInt();
                    byte[] byteRead = new byte[5];
                    byteRead[0] = (byte) 0xAA;
                    byteRead[1] = (byte) 0x72;
                    byteRead[2] = (byte) value;
                  //此处需要考虑兼容性，如果没有传mId，则不对其进行编码
                    if (Utilty.getInstance().isValidofMid(mid)) {
                        byte[] byteMid = new byte[2];
                        byteMid = Utilty.getInstance().int2Bytes(mid, 2);
                        byteRead[3] = byteMid[0];
                        byteRead[4] = byteMid[1];
                    }
                    return byteRead;
                }
                
                
                if (this.cmd.equals("SWITCH")) {
                	this.serviceId="Switch";
                    int status = paras.get("status").asInt();
                    //int timeout = paras.get("timeout").asInt();
                    byte[] byteRead = new byte[6];
                    byteRead[0] = (byte) 0xAA;
                    byteRead[1] = (byte) 0x73;
                    byteRead[2] = (byte) status;
                    byteRead[3] = (byte) 0xFF;// fix me 先固定写死
                  //此处需要考虑兼容性，如果没有传mId，则不对其进行编码
                    if (Utilty.getInstance().isValidofMid(mid)) {
                        byte[] byteMid = new byte[2];
                        byteMid = Utilty.getInstance().int2Bytes(mid, 2);
                        byteRead[4] = byteMid[0];
                        byteRead[5] = byteMid[1];
                    }
                    return byteRead;
                }
                
                
            }

            /*
            平台收到设备的上报数据，根据需要编码ACK，对设备进行响应，如果此处返回null，表示不需要对设备响应。
            * */
            else if (this.msgType.equals("cloudRsp")) {
                byte[] ack = new byte[4];
                ack[0] = (byte) 0xAA;
                ack[1] = (byte) 0xAA;
                ack[2] = (byte) this.errcode;
                ack[3] = (byte) this.hasMore;
                return ack;
            }
            return null;
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return null;
        }
    }
}
