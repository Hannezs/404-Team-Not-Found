package com.catowl.sts.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

/**
 * @program: STS
 * @description: 未定义的设备
 * @author: qqCatOwlbb
 * @create: 2025-11-23 14:03
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnclaimedDevice {
    private String macAddress;
    private LocalDateTime lastSeen;
}
