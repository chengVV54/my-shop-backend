package com.myshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myshop.common.BusinessException;
import com.myshop.config.UserContext;
import com.myshop.entity.User;
import com.myshop.mapper.UserMapper;
import com.myshop.model.LoginRequest;
import com.myshop.model.LoginResponse;
import com.myshop.model.RegisterRequest;
import com.myshop.model.UserVO;
import com.myshop.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 用户业务逻辑：注册、登录、获取当前用户。
 *
 * <p>安全约定：
 * <ul>
 *     <li>密码只以 BCrypt 哈希入库，任何时候都不返回给前端</li>
 *     <li>登录失败时不区分「用户不存在」与「密码错误」，避免用户名被枚举</li>
 *     <li>当前用户身份来自 JWT 解析结果（{@link UserContext}），不接受前端传入 userId</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    /** 注册默认积分，与 users.credit 的默认值保持一致 */
    private static final int INITIAL_CREDIT = 100;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    /**
     * 注册新用户。
     *
     * @param request 用户名与明文密码
     * @return 新用户信息（不含密码）
     * @throws BusinessException 用户名已存在时抛出
     */
    @Transactional
    public UserVO register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (findByUsername(username) != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        // 只存 BCrypt 哈希，明文密码用完即弃
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCredit(INITIAL_CREDIT);
        user.setCreatedAt(LocalDateTime.now());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            // 并发注册同名用户时由 users.username 唯一索引兜底
            throw new BusinessException("用户名已存在");
        }
        return toVO(user);
    }

    /**
     * 登录并签发 JWT。
     *
     * @param request 用户名与明文密码
     * @return token 与用户信息
     * @throws BusinessException 用户名或密码错误时抛出
     */
    public LoginResponse login(LoginRequest request) {
        User user = findByUsername(request.getUsername().trim());
        // 用户不存在与密码错误返回同一提示，避免暴露某个用户名是否注册过
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, toVO(user));
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前用户信息（不含密码）
     * @throws BusinessException 未登录或用户已被删除时抛出
     */
    public UserVO getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "登录用户不存在");
        }
        return toVO(user);
    }

    /**
     * 按用户名精确查询。
     *
     * @return 不存在时返回 null
     */
    private User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 实体转视图对象，刻意不拷贝 {@code password}。
     */
    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setAvatar(user.getAvatar());
        vo.setCredit(user.getCredit());
        vo.setCreatedAt(formatTime(user.getCreatedAt()));
        return vo;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }
}
