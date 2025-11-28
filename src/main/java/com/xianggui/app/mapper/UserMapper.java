package com.xianggui.app.mapper;

import com.xianggui.app.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {
    /**
     * 根据手机号查询用户
     */
    User selectByMobile(@Param("mobile") String mobile);

    /**
     * 根据用户名查询用户
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据ID查询用户
     */
    User selectById(@Param("id") Long id);

    /**
     * 检查手机号是否存在
     */
    int existsMobile(@Param("mobile") String mobile);

    /**
     * 检查用户名是否存在
     */
    int existsUsername(@Param("username") String username);

    /**
     * 插入新用户
     */
    int insert(User user);

    /**
     * 更新用户
     */
    int update(User user);

    /**
     * 更新密码
     */
    int updatePassword(@Param("mobile") String mobile, @Param("passwordHash") String passwordHash);

    /**
     * 更新登录信息
     */
    int updateLoginInfo(@Param("id") Long id, @Param("lastLoginIp") String lastLoginIp);

    /**
     * 更新虚拟形象配置
     */
    int updateAvatarConfig(@Param("id") Long id, @Param("avatarConfig") String avatarConfig);
}
