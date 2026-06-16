package com.zipe.service;

import com.zipe.entity.Account;
import com.zipe.entity.Group;
import com.zipe.repository.AccountRepository;
import com.zipe.repository.GroupRepository;
import com.zipe.vo.AccountVO;
import com.zipe.vo.CreateAccountRequest;
import com.zipe.vo.UpdateAccountRequest;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AccountService} 的預設實作，以注入的 Repository 與密碼編碼器完成帳號管理業務。
 * <p>
 * 刻意不標註 {@code @Service}：此 Bean 由 {@code IamAutoConfiguration} 以
 * {@code @ConditionalOnMissingBean} 註冊，使業務專案可宣告同型別 Bean 覆寫。
 * 所有對外方法均回傳 {@link AccountVO}，避免 JPA 實體與 lazy proxy 漏至交易邊界外。
 * </p>
 *
 * @author Gary.Tsai
 */
public class AccountServiceImpl implements AccountService {

    /** 帳號資料存取層。 */
    private final AccountRepository accountRepository;

    /** 群組資料存取層，供群組指派時查詢群組。 */
    private final GroupRepository groupRepository;

    /** 密碼編碼器，用於將明文密碼雜湊後再儲存。 */
    private final PasswordEncoder passwordEncoder;

    /**
     * 建構帳號服務，注入所需的 Repository 與密碼編碼器。
     *
     * @param accountRepository 帳號 Repository
     * @param groupRepository   群組 Repository
     * @param passwordEncoder   密碼編碼器
     */
    public AccountServiceImpl(AccountRepository accountRepository,
                              GroupRepository groupRepository,
                              PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.groupRepository = groupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public AccountVO createAccount(CreateAccountRequest request) {
        accountRepository.findByUsername(request.getUsername()).ifPresent(existing -> {
            throw new IllegalArgumentException("登入帳號已存在：" + request.getUsername());
        });
        Account account = new Account();
        account.setUsername(request.getUsername());
        if (Objects.nonNull(request.getPassword())) {
            account.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        account.setDisplayName(request.getDisplayName());
        account.setEnabled(Objects.nonNull(request.getEnabled()) ? request.getEnabled() : Boolean.TRUE);
        account.setLocked(Objects.nonNull(request.getLocked()) ? request.getLocked() : Boolean.FALSE);
        LocalDateTime now = LocalDateTime.now();
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        return toVO(accountRepository.save(account));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public AccountVO getAccount(Long id) {
        return toVO(findAccount(id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public AccountVO getAccountByUsername(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("帳號不存在：" + username));
        return toVO(account);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<AccountVO> listAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(this::toVO);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public AccountVO updateAccount(Long id, UpdateAccountRequest request) {
        Account account = findAccount(id);
        if (Objects.nonNull(request.getDisplayName())) {
            account.setDisplayName(request.getDisplayName());
        }
        if (Objects.nonNull(request.getEnabled())) {
            account.setEnabled(request.getEnabled());
        }
        if (Objects.nonNull(request.getLocked())) {
            account.setLocked(request.getLocked());
        }
        account.setUpdatedAt(LocalDateTime.now());
        return toVO(accountRepository.save(account));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void disableAccount(Long id) {
        Account account = findAccount(id);
        account.setEnabled(Boolean.FALSE);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void changePassword(Long id, String rawPassword) {
        Account account = findAccount(id);
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public AccountVO addToGroup(Long accountId, Long groupId) {
        Account account = findAccount(accountId);
        Group group = findGroup(groupId);
        account.getGroups().add(group);
        account.setUpdatedAt(LocalDateTime.now());
        return toVO(accountRepository.save(account));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public AccountVO removeFromGroup(Long accountId, Long groupId) {
        Account account = findAccount(accountId);
        Group group = findGroup(groupId);
        account.getGroups().remove(group);
        account.setUpdatedAt(LocalDateTime.now());
        return toVO(accountRepository.save(account));
    }

    /**
     * 依主鍵取得帳號實體，查無時拋出例外。
     *
     * @param id 帳號主鍵
     * @return 帳號實體
     * @throws IllegalArgumentException 當帳號不存在時拋出
     */
    private Account findAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("帳號不存在：id=" + id));
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
     * 將帳號實體轉換為 {@link AccountVO}，於交易內完成群組代碼展開以避免 lazy loading 例外。
     *
     * @param account 帳號實體
     * @return 帳號視圖
     */
    private AccountVO toVO(Account account) {
        AccountVO vo = new AccountVO();
        vo.setId(account.getId());
        vo.setUsername(account.getUsername());
        vo.setDisplayName(account.getDisplayName());
        vo.setEnabled(account.getEnabled());
        vo.setLocked(account.getLocked());
        vo.setCreatedAt(account.getCreatedAt());
        vo.setUpdatedAt(account.getUpdatedAt());
        vo.setGroupCodes(account.getGroups().stream()
                .map(Group::getCode)
                .collect(Collectors.toSet()));
        return vo;
    }
}
