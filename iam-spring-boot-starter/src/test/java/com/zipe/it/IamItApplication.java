package com.zipe.it;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * iam-starter 整合測試專用啟動類別。
 * <p>
 * 置於獨立套件 {@code com.zipe.it}，使 component scan 不誤掃主程式碼；
 * {@code IamAutoConfiguration} 與 logon 的 {@code SecurityConfiguration} 仍透過
 * auto-configuration 載入，藉此驗證引入即生效的裝配行為。
 * </p>
 *
 * @author Gary.Tsai
 */
@SpringBootApplication
public class IamItApplication {
}
