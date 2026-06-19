package com.zipe.controller;

import com.zipe.service.GroupService;
import com.zipe.vo.CreateGroupRequest;
import com.zipe.vo.GroupVO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 群組（即角色）管理的內建 REST Controller，委派 {@link GroupService} 提供群組 CRUD 與權限掛卸等端點。
 * <p>
 * 路由前綴採用屬性佔位符 {@code ${iam.api.base-path:/api/iam}}，於應用程式啟動時由
 * {@link com.zipe.config.IamProperties} 的 {@code iam.api.base-path} 解析（預設 {@code /api/iam}），
 * 因此本 Controller 的端點預設位於 {@code /api/iam/groups} 之下。
 * </p>
 * <p>
 * 本 Controller 為可選成品，裝配與覆寫機制同 {@link AccountController}。
 * </p>
 *
 * @author Gary.Tsai
 */
@RestController
@RequestMapping("${iam.api.base-path:/api/iam}/groups")
public class GroupController {

    private final GroupService groupService;

    /**
     * 建立群組 Controller。
     *
     * @param groupService 群組管理服務
     */
    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * 建立新群組。
     *
     * @param request 建立群組的請求資料
     * @return 建立完成的群組視圖
     */
    @PostMapping
    public ResponseEntity<GroupVO> create(@RequestBody CreateGroupRequest request) {
        return ResponseEntity.ok(groupService.createGroup(request));
    }

    /**
     * 依主鍵查詢單筆群組。
     *
     * @param id 群組主鍵
     * @return 對應的群組視圖
     */
    @GetMapping("/{id}")
    public ResponseEntity<GroupVO> get(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getGroup(id));
    }

    /**
     * 查詢所有群組。
     *
     * @return 群組視圖清單
     */
    @GetMapping
    public ResponseEntity<List<GroupVO>> list() {
        return ResponseEntity.ok(groupService.listGroups());
    }

    /**
     * 更新群組基本資料；請求中為 {@code null} 的欄位視為不更新（{@code code} 不可變更）。
     *
     * @param id      群組主鍵
     * @param request 更新群組的請求資料
     * @return 更新後的群組視圖
     */
    @PutMapping("/{id}")
    public ResponseEntity<GroupVO> update(@PathVariable Long id,
                                          @RequestBody CreateGroupRequest request) {
        return ResponseEntity.ok(groupService.updateGroup(id, request));
    }

    /**
     * 刪除群組。
     *
     * @param id 群組主鍵
     * @return 無內容回應
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 將指定權限掛載至群組。
     *
     * @param id           群組主鍵
     * @param permissionId 權限主鍵
     * @return 更新後的群組視圖
     */
    @PostMapping("/{id}/permissions/{permissionId}")
    public ResponseEntity<GroupVO> assignPermission(@PathVariable Long id,
                                                    @PathVariable Long permissionId) {
        return ResponseEntity.ok(groupService.assignPermission(id, permissionId));
    }

    /**
     * 將指定權限自群組卸載。
     *
     * @param id           群組主鍵
     * @param permissionId 權限主鍵
     * @return 更新後的群組視圖
     */
    @DeleteMapping("/{id}/permissions/{permissionId}")
    public ResponseEntity<GroupVO> revokePermission(@PathVariable Long id,
                                                    @PathVariable Long permissionId) {
        return ResponseEntity.ok(groupService.revokePermission(id, permissionId));
    }
}
