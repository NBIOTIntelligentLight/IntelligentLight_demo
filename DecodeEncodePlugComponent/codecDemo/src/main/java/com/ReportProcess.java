package com;

import java.io.UnsupportedEncodingException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ReportProcess {
    private String identifier = "123";

    private String msgType = "deviceReq";
    private int hasMore = 0;
    private int errcode = 0;
    
    private int batteryLevel = 0;
    private int batteryThreshold = 0;
    
    private int conf_status = 0;
    
    private int Illumination = 0;
    
    private int motion = 0;
    
    private int switch_status = 0;
    
    
    
    
    private double voltage = 0.0;
    private int current = 0;
    private double frequency = 0.0;
    private double powerfactor = 0.0;
    private int temperature = 0;
    private byte bDeviceReq = 0x00;
    private byte bDeviceRsp = 0x01;
    private byte noMid = 0x00;
    private byte hasMid = 0x01;
    private boolean isContainMid = false;
    private int mid = 0;

    /**
     * @param binaryData 设备发送给平台coap报文的payload部分
     *                   本例入参：AA 72 00 00 32 08 8D 03 20 62 33 99
     *                   byte[0]--byte[1]:  AA 72 命令头
     *                   byte[2]:   00 mstType 00表示设备上报数据deviceReq
     *                   byte[3]:   00 hasMore  0表示没有后续数据，1表示有后续数据，不带按照0处理
     *                   byte[4]--byte[11]:服务数据，根据需要解析
     * @return
     * @throws UnsupportedEncodingException 
     */
    public ReportProcess(byte[] binaryData) throws UnsupportedEncodingException {
        // identifier参数可以根据入参的码流获得，本例指定默认值123
        identifier ="123";  
        /*
        如果是设备上报数据，返回格式为
        {
            "identifier":"123",
            "msgType":"deviceReq",
            "hasMore":0,
            "data":[ { "serviceId":"waterMeter", "serviceData":{"a":1} } ]
	    }
	    */
        if (binaryData[2] == bDeviceReq) {
            msgType = "deviceReq";
            hasMore = binaryData[3];
            //解析数据
            batteryLevel = binaryData[4];
            batteryThreshold = binaryData[5];
            conf_status = binaryData[6];
            Illumination = (binaryData[7]<<8)+(binaryData[8]& 0xFF);
            motion = binaryData[9];
            switch_status=binaryData[10];
        }
        /*
        如果是设备对平台命令的应答，返回格式为：
       {
            "identifier":"123",
            "msgType":"deviceRsp",
            "errcode":0,
            "body" :{****} 特别注意该body体为一层json结构。
        }
	    */
        else if (binaryData[2] == bDeviceRsp) {
            msgType = "deviceRsp";
            errcode = binaryData[3];
            //此处需要考虑兼容性，如果没有传mId，则不对其进行解码
            if (binaryData[4] == hasMid) {
                mid = Utilty.getInstance().bytes2Int(binaryData, 5, 2);
                if (Utilty.getInstance().isValidofMid(mid)) {
                    isContainMid = true;
        } 
            }
        } else {
            return;
        }

        
        
        
//        voltage = (double) (((binaryData[5] << 8) + (binaryData[6] & 0xFF)) * 0.1f);
//        current = (binaryData[7] << 8) + binaryData[8];
//        powerfactor = (double) (binaryData[9] * 0.01);
//        frequency = (double) binaryData[10] * 0.1f + 45;
//        temperature = (int) binaryData[11] & 0xFF - 128;

    }

    public ObjectNode toJsonNode() {
        try {
            //组装body体
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode arrynode = mapper.createArrayNode();

            //battery
            ObjectNode batteryNode = mapper.createObjectNode();
            batteryNode.put("serviceId", "Battery");
            ObjectNode batteryData = mapper.createObjectNode();
            batteryData.put("batteryLevel", this.batteryLevel);
            batteryData.put("batteryThreshold", this.batteryThreshold);
            batteryNode.put("serviceData", batteryData);
            arrynode.add(batteryNode);

            //configuration
            ObjectNode configurationNode = mapper.createObjectNode();
            configurationNode.put("serviceId", "Configuration");
            ObjectNode configurationData = mapper.createObjectNode();
            configurationData.put("status", this.conf_status);
            configurationNode.put("serviceData", configurationData);
            arrynode.add(configurationNode);

            //Illumination
            ObjectNode IlluminationNode = mapper.createObjectNode();
            IlluminationNode.put("serviceId", "Illumination");
            ObjectNode IlluminationData = mapper.createObjectNode();
            IlluminationData.put("Illumination", this.Illumination);
            IlluminationNode.put("serviceData", IlluminationData);
            arrynode.add(IlluminationNode);
            
            
            //motion
            ObjectNode motionNode = mapper.createObjectNode();
            motionNode.put("serviceId", "Motion");
            ObjectNode motionData = mapper.createObjectNode();
            motionData.put("motion", this.motion);
            motionNode.put("serviceData", motionData);
            arrynode.add(motionNode);
            
            
            
            //switch
            ObjectNode switchNode = mapper.createObjectNode();
            switchNode.put("serviceId", "Switch");
            ObjectNode switchData = mapper.createObjectNode();
            switchData.put("switch_status", this.switch_status);
            switchNode.put("serviceData", switchData);
            arrynode.add(switchNode);

            
            ObjectNode root = mapper.createObjectNode();

            //根据msgType字段组装消息体
            root.put("identifier", this.identifier);
            root.put("msgType", this.msgType);

            /*
            如果是设备上报数据，返回格式为
            {
                "identifier":"123",
                "msgType":"deviceReq",
                "hasMore":0,
                "data":[ { "serviceId":"waterMeter", "serviceData":{"a":1} } ]
            }

            如果是设备对平台命令的应答，返回格式为：
           {
                "identifier":"123",
                "msgType":"deviceRsp",
                "errcode":0,
                "body" :{****} 特别注意该body体为一层json结构。
            }
	        */
            if (this.msgType.equals("deviceReq")) {
                root.put("hasMore", this.hasMore);
                root.put("data", arrynode);
            } else {
                root.put("errcode", this.errcode);
                //此处需要考虑兼容性，如果没有传mid，则不对其进行解码
                if (isContainMid) {
                    root.put("mid", this.mid);//mid
                }
                //组装body体，只能为ObjectNode对象
                ObjectNode body = mapper.createObjectNode();
                body.put("result", 0);
                root.put("body", body);
            }
            return root;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}


