package com.zipe.controller;

import com.zipe.annotation.ResponseResultBody;
import com.zipe.dto.Result;
import com.zipe.dto.User;
import com.zipe.exception.ResultException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RESTful API 範例控制器。
 *
 * <p>示範 {@link ResponseResultBody} 統一回應包裝機制的各種使用情境，
 * 包含正常回傳、包裝成 {@link Result} 物件、拋出一般例外，
 * 以及拋出自訂 {@link ResultException} 的處理流程。
 * 所有端點皆掛載於 {@code /rest} 路徑，並固定輸出 {@code application/json}。</p>
 */
@RestController
@RequestMapping(value = "/rest", produces = "application/json")
@ResponseResultBody
public class RestfulController {

    /**
     * 預設示範用靜態資料，於類別載入時初始化一次，供多個端點共用。
     */
    private static final HashMap<String, Object> INFO;

    static {
        // 初始化共用的示範資料，避免每次請求重複建立物件
        INFO = new HashMap<>();
        INFO.put("name", "galaxy");
        INFO.put("age", "70");
    }

    /**
     * 回傳示範用 Map 資料。
     *
     * <p>直接回傳原始 Map，由 {@link ResponseResultBody} 攔截並包裝成統一格式。</p>
     *
     * @return 包含示範資料的 HashMap
     */
    @GetMapping(value = "/hello")
    public HashMap<String, Object> hello() {
        return INFO;
    }

    /**
     * 回傳已包裝成 {@link Result} 的示範資料。
     *
     * <p>由呼叫端自行封裝為 {@link Result}，{@link ResponseResultBody} 不會重複包裝。</p>
     *
     * @return 成功狀態的 {@link Result}，內含示範資料
     */
    @GetMapping("/result")
    public Result<Map<String, Object>> helloResult() {
        return Result.success(INFO);
    }

    /**
     * 示範拋出一般 {@link Exception} 的錯誤處理。
     *
     * <p>{@link ResponseResultBody} 的 Advice 會攔截此例外並轉換為統一錯誤回應。</p>
     *
     * @return 此方法永遠不會正常回傳
     * @throws Exception 固定拋出，用於測試全域例外處理機制
     */
    @GetMapping("/helloError")
    public HashMap<String, Object> helloError() throws Exception {
        throw new Exception("helloError");
    }

    /**
     * 示範拋出自訂 {@link ResultException} 的錯誤處理。
     *
     * <p>與 {@code helloError()} 的差異在於使用業務層自訂例外，
     * 可攜帶特定的 {@code ResultStatus} 錯誤碼，由 Advice 轉換為對應的統一回應。</p>
     *
     * @return 此方法永遠不會正常回傳
     * @throws ResultException 固定拋出，用於測試自訂例外的處理流程
     */
    @GetMapping("/helloMyError")
    public HashMap<String, Object> helloMyError() throws Exception {
        throw new ResultException();
    }

    /**
     * 示範回傳純字串時的統一包裝效果。
     *
     * @return 固定字串 {@code "helloString"}
     */
    @GetMapping(value = "/testString")
    public String testString() {
        return "helloString";
    }

    /**
     * 示範回傳整數時的統一包裝效果。
     *
     * @return 固定整數 {@code 123}
     */
    @GetMapping(value = "/testInt")
    public Integer testInt() {
        return 123;
    }

    /**
     * 示範回傳物件時的統一包裝效果。
     *
     * <p>建立一個帶有預設帳密的 {@link User}，用於驗證物件序列化與回應包裝是否正常。</p>
     *
     * @return 帶有示範帳號資訊的 {@link User} 物件
     */
    @GetMapping(value = "/testUser")
    public User testUser() {
        User user = new User();
        user.setUsername("Gary");
        user.setPassword("password");
        return user;
    }
}
