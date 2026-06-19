package com.zipe.service;

import com.zipe.vo.CreateGroupRequest;
import com.zipe.vo.GroupVO;
import java.util.List;

/**
 * 群組（即角色）管理服務，提供群組的 CRUD 與群組／權限掛卸等核心業務。
 * <p>
 * 所有方法一律回傳 {@link GroupVO} 等 DTO，不外漏 {@link com.zipe.entity.Group}
 * JPA 實體與其 lazy proxy。實作以 {@code @ConditionalOnMissingBean} 註冊，業務專案
 * 可宣告同型別 Bean 覆寫。
 * </p>
 *
 * @author Gary.Tsai
 */
public interface GroupService {

    /**
     * 建立新群組。
     *
     * @param request 建立群組的請求資料
     * @return 建立完成的群組視圖
     * @throws IllegalArgumentException 當群組代碼已存在時拋出
     */
    GroupVO createGroup(CreateGroupRequest request);

    /**
     * 依主鍵查詢單筆群組。
     *
     * @param id 群組主鍵
     * @return 對應的群組視圖
     * @throws IllegalArgumentException 當群組不存在時拋出
     */
    GroupVO getGroup(Long id);

    /**
     * 查詢所有群組。
     *
     * @return 群組視圖清單
     */
    List<GroupVO> listGroups();

    /**
     * 更新群組基本資料；請求中為 {@code null} 的欄位視為不更新（{@code code} 不可變更）。
     *
     * @param id      群組主鍵
     * @param request 更新群組的請求資料
     * @return 更新後的群組視圖
     * @throws IllegalArgumentException 當群組不存在時拋出
     */
    GroupVO updateGroup(Long id, CreateGroupRequest request);

    /**
     * 刪除群組。
     *
     * @param id 群組主鍵
     * @throws IllegalArgumentException 當群組不存在時拋出
     */
    void deleteGroup(Long id);

    /**
     * 將指定權限掛載至群組。
     *
     * @param groupId      群組主鍵
     * @param permissionId 權限主鍵
     * @return 更新後的群組視圖
     * @throws IllegalArgumentException 當群組或權限不存在時拋出
     */
    GroupVO assignPermission(Long groupId, Long permissionId);

    /**
     * 將指定權限自群組卸載。
     *
     * @param groupId      群組主鍵
     * @param permissionId 權限主鍵
     * @return 更新後的群組視圖
     * @throws IllegalArgumentException 當群組或權限不存在時拋出
     */
    GroupVO revokePermission(Long groupId, Long permissionId);
}
