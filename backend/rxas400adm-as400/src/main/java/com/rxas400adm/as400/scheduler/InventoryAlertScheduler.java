package com.rxas400adm.as400.scheduler;

import com.rxas400adm.as400.context.As400ServerContextHolder;
import com.rxas400adm.as400.entity.IbmiSystem;
import com.rxas400adm.as400.service.IBpcsSupplyChainService;
import com.rxas400adm.as400.service.IbmiSystemService;
import com.rxas400adm.as400.vo.BpcsInventoryAlertVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存预警定时扫描：每 5 分钟检查所有 AS400 服务器的 BPCS 库存，
 * 将低于安全库存的物料通过 WebSocket STOMP 推送到 /topic/bpcs/inventory-alert。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryAlertScheduler {

    private final IBpcsSupplyChainService supplyChainService;
    private final IbmiSystemService ibmiSystemService;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelayString = "${rxas400.bpcs.alert-interval:300000}", initialDelay = 60000)
    public void scanInventoryAlerts() {
        log.debug("[库存预警] 开始扫描...");
        List<IbmiSystem> servers = ibmiSystemService.list();
        List<Map<String, Object>> allAlerts = new ArrayList<>();

        for (IbmiSystem server : servers) {
            if (!Boolean.TRUE.equals(server.getEnabled())) {
                continue;
            }
            try {
                As400ServerContextHolder.setServerId(server.getId());
                List<BpcsInventoryAlertVO> alerts = supplyChainService.inventoryAlerts("001", 100);
                for (BpcsInventoryAlertVO alert : alerts) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("serverId", server.getId());
                    msg.put("serverName", server.getName());
                    msg.put("item", alert.item());
                    msg.put("description", alert.description());
                    msg.put("warehouse", alert.warehouse());
                    msg.put("uom", alert.uom());
                    msg.put("onHand", alert.onHand());
                    msg.put("allocated", alert.allocated());
                    msg.put("available", alert.available());
                    msg.put("safetyStock", alert.safetyStock());
                    msg.put("deficit", alert.deficit());
                    msg.put("timestamp", LocalDateTime.now().toString());
                    allAlerts.add(msg);
                }
            } catch (Exception e) {
                log.warn("[库存预警] 服务器 {} 扫描失败: {}", server.getName(), e.getMessage());
            } finally {
                As400ServerContextHolder.clear();
            }
        }

        if (!allAlerts.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/bpcs/inventory-alert", allAlerts);
            log.info("[库存预警] 推送 {} 条预警到 WebSocket", allAlerts.size());
        }
    }
}
