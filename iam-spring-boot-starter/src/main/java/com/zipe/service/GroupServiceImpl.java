package com.zipe.service;

import com.zipe.entity.Group;
import com.zipe.entity.Permission;
import com.zipe.repository.GroupRepository;
import com.zipe.repository.PermissionRepository;
import com.zipe.vo.CreateGroupRequest;
import com.zipe.vo.GroupVO;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link GroupService} 的預設實作，以注入的 Repository 完成群組與群組／權限掛卸業務。
 * <p>
 * 刻意不標註 {@code @Service}：此 Bean 由 {@code IamAutoConfiguration} 以
 * {@code @ConditionalOnMissingBean} 註冊，使業務專案可宣告同型別 Bean 覆寫。
 * 所有對外方法均回傳 {@link GroupVO}，避免 JPA 實體與 lazy proxy 漏至交易邊界外。
 * </p>
 *
 * @author Gary.Tsai
 */
public class GroupServiceImpl implements GroupService {

    /** 群組資料存取層。 */
    private final GroupRepository groupRepository;

    /** 權限資料存取層，供群組掛卸權限時查詢權限。 */
    private final PermissionRepository permissionRepository;

    /**
     * 建構群組服務，注入所需的 Repository。
     *
     * @param groupRepository      群組 Repository
     * @param permissionRepository 權限 Repository
     */
    public GroupServiceImpl(GroupRepository groupRepository,
                            PermissionRepository permissionRepository) {
        this.groupRepository = groupRepository;
        this.permissionRepository = permissionRepository;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public GroupVO createGroup(CreateGroupRequest request) {
        groupRepository.findByCode(request.getCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("群組代碼已存在：" + request.getCode());
        });
        Group group = new Group();
        group.setCode(request.getCode());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setEnabled(Objects.nonNull(request.getEnabled()) ? request.getEnabled() : Boolean.TRUE);
        return toVO(groupRepository.save(group));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public GroupVO getGroup(Long id) {
        return toVO(findGroup(id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<GroupVO> listGroups() {
        return groupRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public GroupVO updateGroup(Long id, CreateGroupRequest request) {
        Group group = findGroup(id);
        if (Objects.nonNull(request.getName())) {
            group.setName(request.getName());
        }
        if (Objects.nonNull(request.getDescription())) {
            group.setDescription(request.getDescription());
        }
        if (Objects.nonNull(request.getEnabled())) {
            group.setEnabled(request.getEnabled());
        }
        return toVO(groupRepository.save(group));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteGroup(Long id) {
        Group group = findGroup(id);
        groupRepository.delete(group);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public GroupVO assignPermission(Long groupId, Long permissionId) {
        Group group = findGroup(groupId);
        Permission permission = findPermission(permissionId);
        group.getPermissions().add(permission);
        return toVO(groupRepository.save(group));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public GroupVO revokePermission(Long groupId, Long permissionId) {
        Group group = findGroup(groupId);
        Permission permission = findPermission(permissionId);
        group.getPermissions().remove(permission);
        return toVO(groupRepository.save(group));
    }

    /**
     * 依主鍵取得群組實體，查無時拋出例外。
     *
     * @param id 群組主鍵
     * @return 群組實體
     * @throws IllegalArgumentException 當群組不存在時拋出
     */
    private Group findGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("群組不存在：id=" + id));
    }

    /**
     * 依主鍵取得權限實體，查無時拋出例外。
     *
     * @param id 權限主鍵
     * @return 權限實體
     * @throws IllegalArgumentException 當權限不存在時拋出
     */
    private Permission findPermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("權限不存在：id=" + id));
    }

    /**
     * 將群組實體轉換為 {@link GroupVO}，於交易內完成權限代碼展開以避免 lazy loading 例外。
     *
     * @param group 群組實體
     * @return 群組視圖
     */
    private GroupVO toVO(Group group) {
        GroupVO vo = new GroupVO();
        vo.setId(group.getId());
        vo.setCode(group.getCode());
        vo.setName(group.getName());
        vo.setDescription(group.getDescription());
        vo.setEnabled(group.getEnabled());
        vo.setPermissionCodes(group.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet()));
        return vo;
    }
}
