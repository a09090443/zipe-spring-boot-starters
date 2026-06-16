package com.zipe.service;

import com.zipe.entity.Permission;
import com.zipe.repository.PermissionRepository;
import com.zipe.vo.CreatePermissionRequest;
import com.zipe.vo.PermissionVO;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link PermissionService} 的預設實作，以注入的 Repository 完成權限管理業務。
 * <p>
 * 刻意不標註 {@code @Service}：此 Bean 由 {@code IamAutoConfiguration} 以
 * {@code @ConditionalOnMissingBean} 註冊，使業務專案可宣告同型別 Bean 覆寫。
 * 所有對外方法均回傳 {@link PermissionVO}，避免 JPA 實體漏至交易邊界外。
 * </p>
 *
 * @author Gary.Tsai
 */
public class PermissionServiceImpl implements PermissionService {

    /** 權限資料存取層。 */
    private final PermissionRepository permissionRepository;

    /**
     * 建構權限服務，注入所需的 Repository。
     *
     * @param permissionRepository 權限 Repository
     */
    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public PermissionVO createPermission(CreatePermissionRequest request) {
        permissionRepository.findByCode(request.getCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("權限代碼已存在：" + request.getCode());
        });
        Permission permission = new Permission();
        permission.setCode(request.getCode());
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());
        permission.setEnabled(Objects.nonNull(request.getEnabled()) ? request.getEnabled() : Boolean.TRUE);
        return toVO(permissionRepository.save(permission));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PermissionVO getPermission(Long id) {
        return toVO(findPermission(id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<PermissionVO> listPermissions() {
        return permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public PermissionVO updatePermission(Long id, CreatePermissionRequest request) {
        Permission permission = findPermission(id);
        if (Objects.nonNull(request.getName())) {
            permission.setName(request.getName());
        }
        if (Objects.nonNull(request.getDescription())) {
            permission.setDescription(request.getDescription());
        }
        if (Objects.nonNull(request.getEnabled())) {
            permission.setEnabled(request.getEnabled());
        }
        return toVO(permissionRepository.save(permission));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deletePermission(Long id) {
        Permission permission = findPermission(id);
        permissionRepository.delete(permission);
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
     * 將權限實體轉換為 {@link PermissionVO}。
     *
     * @param permission 權限實體
     * @return 權限視圖
     */
    private PermissionVO toVO(Permission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setCode(permission.getCode());
        vo.setName(permission.getName());
        vo.setDescription(permission.getDescription());
        vo.setEnabled(permission.getEnabled());
        return vo;
    }
}
