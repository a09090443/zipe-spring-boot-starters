package com.zipe.controller;

import com.zipe.service.PermissionService;
import com.zipe.vo.CreatePermissionRequest;
import com.zipe.vo.PermissionVO;
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
 * 權限管理的內建 REST Controller，委派 {@link PermissionService} 提供權限 CRUD 端點。
 * <p>
 * 路由前綴採用屬性佔位符 {@code ${iam.api.base-path:/api/iam}}，於應用程式啟動時由
 * {@link com.zipe.config.IamProperties} 的 {@code iam.api.base-path} 解析（預設 {@code /api/iam}），
 * 因此本 Controller 的端點預設位於 {@code /api/iam/permissions} 之下。
 * </p>
 * <p>
 * 本 Controller 為可選成品，裝配與覆寫機制同 {@link AccountController}。
 * </p>
 *
 * @author Gary.Tsai
 */
@RestController
@RequestMapping("${iam.api.base-path:/api/iam}/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 建立權限 Controller。
     *
     * @param permissionService 權限管理服務
     */
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 建立新權限。
     *
     * @param request 建立權限的請求資料
     * @return 建立完成的權限視圖
     */
    @PostMapping
    public ResponseEntity<PermissionVO> create(@RequestBody CreatePermissionRequest request) {
        return ResponseEntity.ok(permissionService.createPermission(request));
    }

    /**
     * 依主鍵查詢單筆權限。
     *
     * @param id 權限主鍵
     * @return 對應的權限視圖
     */
    @GetMapping("/{id}")
    public ResponseEntity<PermissionVO> get(@PathVariable Long id) {
        return ResponseEntity.ok(permissionService.getPermission(id));
    }

    /**
     * 查詢所有權限。
     *
     * @return 權限視圖清單
     */
    @GetMapping
    public ResponseEntity<List<PermissionVO>> list() {
        return ResponseEntity.ok(permissionService.listPermissions());
    }

    /**
     * 更新權限基本資料；請求中為 {@code null} 的欄位視為不更新（{@code code} 不可變更）。
     *
     * @param id      權限主鍵
     * @param request 更新權限的請求資料
     * @return 更新後的權限視圖
     */
    @PutMapping("/{id}")
    public ResponseEntity<PermissionVO> update(@PathVariable Long id,
                                               @RequestBody CreatePermissionRequest request) {
        return ResponseEntity.ok(permissionService.updatePermission(id, request));
    }

    /**
     * 刪除權限。
     *
     * @param id 權限主鍵
     * @return 無內容回應
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}
