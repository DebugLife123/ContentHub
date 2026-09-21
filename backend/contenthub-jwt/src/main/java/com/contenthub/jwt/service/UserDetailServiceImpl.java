package com.contenthub.jwt.service;

import com.contenthub.common.domain.dos.UserDO;
import com.contenthub.common.domain.mapper.UserMapper;
import com.contenthub.jwt.model.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 从数据库加载用户，并带上真实角色（计划 Day 17：数据库增加 role，后端接口做角色判断）。
 *
 * <p>阶段 6 Day 51 增加状态判断：被管理员禁用的账号 {@code enabled=false}，
 * DaoAuthenticationProvider 会据此抛出 DisabledException，登录被拒。
 * 注意这里不能直接抛异常，否则「用户名不存在」与「账号被禁用」在响应上无法区分。</p>
 */
@Service
@Slf4j
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDO userDO = userMapper.findByUsername(username);

        if (Objects.isNull(userDO)) {
            throw new UsernameNotFoundException("该用户不存在");
        }

        boolean enabled = !"DISABLED".equalsIgnoreCase(userDO.getStatus());

        return new LoginUser(userDO.getId(), userDO.getUsername(), userDO.getPassword(),
                userDO.getRole(), enabled);
    }
}
