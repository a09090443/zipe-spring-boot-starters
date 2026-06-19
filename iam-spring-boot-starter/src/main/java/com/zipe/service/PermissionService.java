package com.zipe.service;

import com.zipe.vo.CreatePermissionRequest;
import com.zipe.vo.PermissionVO;
import java.util.List;

/**
 * 權限管理服務，提供權限的 CRUD 與查詢等核心業務。
 * <p>
 * 所有方法一律回傳 {@link PermissionVO} 等 DTO，不外漏 {@link com.zipe.entity.Permission}
 * JPA 實體。實作以 {@code @ConditionalOnMissingBean} 註冊，業務專案可宣告同型別 Bean 覆寫。
 * </p>
 *
 * @author Gary.Tsai
 */
public interface PermissionService {

    /**
     * 建立新權限。
     *
     * @param request 建立權限的請求資料
     * @return 建立完成的權限視圖
     * @throws IllegalArgumentException 當權限代碼已存在時拋出
     */
    PermissionVO createPermission(CreatePermissionRequest request);

    /**
     * 依主鍵查詢單筆權限。
     *
     * @param id 權限主鍵
     * @return 對應的權限視圖
     * @throws IllegalArgumentException 當權限不存在時拋出
     */
    PermissionVO getPermission(Long id);

    /**
     * 查詢所有權限。
     *
     * @return 權限視圖清單
     */
    List<PermissionVO> listPermissions();

    /**
     * 更新權限基本資料；請求中為 {@code null} 的欄位視為不更新（{@code code} 不可變更）。
     *
     * @param id      權限主鍵
     * @param request 更新權限的請求資料
     * @return 更新後的權限視圖
     * @throws IllegalArgumentException 當權限不存在時拋出
     */
    PermissionVO updatePermission(Long id, CreatePermissionRequest request);

    /**
     * 刪除權限。
     *
     * @param id 權限主鍵
     * @throws IllegalArgumentException 當權限不存在時拋出
     */
    void deletePermission(Long id);
}
