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
 * <p>改造前这里把 authorities 写死成 ADMIN，导致任何登录用户都拥有管理员权限。</p>
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

        return new LoginUser(userDO.getId(), userDO.getUsername(), userDO.getPassword(), userDO.getRole());
    }
}
