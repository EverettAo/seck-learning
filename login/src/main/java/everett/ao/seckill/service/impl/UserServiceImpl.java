package everett.ao.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mysql.cj.util.StringUtils;
import everett.ao.seckill.constant.TokenConstant;
import everett.ao.seckill.entity.UserEntity;
import everett.ao.seckill.exception.GlobalException;
import everett.ao.seckill.mapper.UserMapper;
import everett.ao.seckill.service.RedisService;
import everett.ao.seckill.service.UserService;
import everett.ao.seckill.utils.CodeMsg;
import everett.ao.seckill.utils.MD5Utils;
import everett.ao.seckill.utils.UserTokenPrefix;
import everett.ao.seckill.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {
    @Autowired
    RedisService redisService;

    @Override
    public UserEntity login(LoginVo loginVo) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<UserEntity>().eq("phone", loginVo.getMobile());
        UserEntity userEntity = this.baseMapper.selectOne(wrapper);
        if (userEntity == null)
            throw new GlobalException(CodeMsg.MOBILTNOTEXIST);   // 因为有全局的异常处理器，且是RuntimeException的，所以任何地方都可以直接抛GolbalException
        if (!userEntity.getPassword().equals(MD5Utils.submitPass2dbPass(loginVo.getPassword(), userEntity.getSalt())))
            throw new GlobalException(CodeMsg.PASSWORDERROR);
        return userEntity;
    }

    @Override
    public UserEntity getByToken(HttpServletResponse response, String token) {
        if (StringUtils.isNullOrEmpty(token)) return null;
        UserEntity user = redisService.get(UserTokenPrefix.getByToken, token, UserEntity.class);
        if (user != null) addCookie(response, token, user);
        return user;
    }

    private void addCookie(HttpServletResponse response, String token, UserEntity user) {
        redisService.set(UserTokenPrefix.getByToken, token, user);
        Cookie cookie = new Cookie(TokenConstant.Cookie_Token_Key, token);
        cookie.setMaxAge(UserTokenPrefix.getByToken.expireSeconds());   // cookie和redis中的key的过期时间一致
        cookie.setPath(TokenConstant.Token_Path);
        response.addCookie(cookie);
    }
}
