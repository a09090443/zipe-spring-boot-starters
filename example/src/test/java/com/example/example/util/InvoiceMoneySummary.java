package com.example.example.util;

import lombok.Data;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/12/23 上午 11:25
 **/
@Data
public class InvoiceMoneySummary {

    // 公司統編
    private String companyId;
    // 發票類型
    private String type;
    // 含稅
    private Long includeTaxMoney = 0L;
    // 稅額
    private Long taxMoney = 0L;
    // 非經海關
    private Long notViaCustomsMoney = 0L;
    // 免稅
    private Long freeTaxMoney = 0L;
    // 空白發票總計
    private Integer unusedInvoiceCount = 0;
    // 總開立發票數
    private Integer quantity = 0;

}
