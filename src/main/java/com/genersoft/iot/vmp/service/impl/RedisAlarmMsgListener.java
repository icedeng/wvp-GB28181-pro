package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;


@Component
public class RedisAlarmMsgListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisAlarmMsgListener.class);

    @Autowired
    private ISIPCommander commander;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private IVideoManagerStorage storage;

    @Override
    public void onMessage(Message message, byte[] bytes) {
        logger.info("收到来自REDIS的ALARM通知： {}", new String(message.getBody()));
        AlarmChannelMessage alarmChannelMessage = JSON.parseObject(message.getBody(), AlarmChannelMessage.class);
        if (alarmChannelMessage == null) {
            logger.warn("[REDIS的ALARM通知]消息解析失败");
            return;
        }
        String gbId = alarmChannelMessage.getGbId();
        Device device = storage.queryVideoDevice(gbId);
        ParentPlatform platform = storage.queryParentPlatByServerGBId(gbId);

        DeviceAlarm deviceAlarm = new DeviceAlarm();
        deviceAlarm.setCreateTime(DateUtil.getNow());
        deviceAlarm.setChannelId(gbId);
        deviceAlarm.setAlarmDescription(alarmChannelMessage.getAlarmDescription());
        deviceAlarm.setAlarmMethod("" + alarmChannelMessage.getAlarmSn());
        deviceAlarm.setAlarmPriority("1");
        deviceAlarm.setAlarmTime(DateUtil.getNow());
        deviceAlarm.setAlarmType("1");
        deviceAlarm.setLongitude(0);
        deviceAlarm.setLatitude(0);


        if (device != null && platform == null) {
            commander.sendAlarmMessage(device, deviceAlarm);
        }else if (device == null && platform != null){
            commanderForPlatform.sendAlarmMessage(platform, deviceAlarm);
        }else {
           logger.warn("无法确定" + gbId + "是平台还是设备");
        }
    }
}
