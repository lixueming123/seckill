package com.lxm.seckill.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lxm.seckill.config.AccessLimit;
import com.lxm.seckill.entity.Goods;
import com.lxm.seckill.entity.SeckillGoods;
import com.lxm.seckill.entity.User;
import com.lxm.seckill.service.GoodsService;
import com.lxm.seckill.service.SeckillGoodsService;
import com.lxm.seckill.service.UserService;
import com.lxm.seckill.utils.Const;
import com.lxm.seckill.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    UserService userService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    SeckillGoodsService seckillGoodsService;

    @Autowired
    SeckillController seckillController;
    /*
    * 页面静态化
    * */
    @Autowired
    ThymeleafViewResolver thymeleafViewResolver;

    @Autowired
    RedisTemplate<String,Object> redisTemplate;

    @GetMapping("/list")
    @AccessLimit
    public RespBean list(User user) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        Object goodsList = redisTemplate.opsForValue().get("goodsList");
        if (goodsList != null) {
            return RespBean.success(goodsList);
        }

        List<GoodsVo> goodsVoList = goodsService.findGoodsVoList();
        redisTemplate.opsForValue().set("goodsList", goodsVoList, 5, TimeUnit.SECONDS);
        return RespBean.success(goodsVoList);
    }

    @GetMapping("/admin/list")
    @AccessLimit
    public RespBean adminList(User user) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        List<GoodsVo> goodsVoList = goodsService.findGoodsVoList();
        return RespBean.success(goodsVoList);
    }

    @GetMapping("/detail/{goodsId}")
    @AccessLimit
    public RespBean detail(User user, @PathVariable Long goodsId) {
        DetailVo detailVo = new DetailVo();

        GoodsVo goodsVo = goodsService.findGoodsVoById(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();

        detailVo.setGoodsVo(goodsVo);

        int secKillStatus;
        int remainSeconds;
        //
        if (nowDate.before(startDate)) { // 秒杀未开始
            secKillStatus = 0;
            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if (nowDate.after(endDate)) { // 秒杀已经结束
            secKillStatus = 2;
            remainSeconds = -1;
        } else { // 秒杀进行中
            secKillStatus = 1;
            remainSeconds = 0;
        }

        detailVo.setUser(user);
        detailVo.setRemainSeconds(remainSeconds);
        detailVo.setSecKillStatus(secKillStatus);

        return RespBean.success(detailVo);
    }

    @GetMapping("/delete/{goodsId}")
    @AccessLimit
    public RespBean delete(User user, @PathVariable("goodsId")Long goodsId) {
        goodsService.removeById(goodsId);
        seckillGoodsService.remove(new QueryWrapper<SeckillGoods>().eq("goods_id", goodsId));
        redisTemplate.delete(Const.SECKILL_GOODS_PREFIX + goodsId);
        SeckillController.emptyStock.remove(goodsId);
        return RespBean.success();
    }

    @PostMapping("/edit")
    @AccessLimit
    public RespBean add(@RequestBody GoodsVoRequest goodsVo) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        SeckillGoods seckillGoods = new SeckillGoods();
        Goods goods = new Goods();
        if (goodsVo.getId() == null) {
            goods.setGoodsDetail(goodsVo.getGoodsDetail());
            if (goodsVo.getGoodsImg() == null) goods.setGoodsImg("/img/iphone12.png");
            else goods.setGoodsImg(goodsVo.getGoodsImg());
            goods.setGoodsPrice(goodsVo.getGoodsPrice());
            goods.setGoodsStock(goodsVo.getStockCount());
            goods.setGoodsName(goodsVo.getGoodsName());
            goods.setGoodsTitle(goodsVo.getGoodsTitle());
            goodsService.saveOrUpdate(goods);

            seckillGoods.setSeckillPrice(goodsVo.getSeckillPrice());
            seckillGoods.setGoodsId(goods.getId());
            seckillGoods.setStockCount(goodsVo.getStockCount());
            seckillGoods.setStartDate(new Date());
            seckillGoods.setEndDate(new Date(new Date().getTime() + 7L * 1000 * 60 * 60 * 24));
            seckillGoodsService.saveOrUpdate(seckillGoods);
        }
        else {
            goods.setId(goodsVo.getId());
            goods.setGoodsDetail(goodsVo.getGoodsDetail());
            if (goodsVo.getGoodsImg() != null) goods.setGoodsImg(goodsVo.getGoodsImg());
            goods.setGoodsPrice(goodsVo.getGoodsPrice());
            goods.setGoodsStock(goodsVo.getStockCount());
            goods.setGoodsName(goodsVo.getGoodsName());
            goods.setGoodsTitle(goodsVo.getGoodsTitle());
            goodsService.saveOrUpdate(goods);

            seckillGoods.setId(goodsVo.getId());
            seckillGoods.setGoodsId(goods.getId());
            seckillGoods.setStockCount(goodsVo.getStockCount());
            seckillGoods.setStartDate(new Date());
            seckillGoods.setEndDate(new Date(new Date().getTime() + 7L * 1000 * 60 * 60 * 24));
            seckillGoodsService.saveOrUpdate(seckillGoods);
        }

        SeckillController.emptyStock.put(goods.getId(), false);
        ops.set(Const.SECKILL_GOODS_PREFIX + goods.getId(), seckillGoods.getStockCount(),
                seckillGoods.getEndDate().getTime() - seckillGoods.getStartDate().getTime() + 1000 * 60,
                TimeUnit.MILLISECONDS
        );

        return RespBean.success();
    }

    /**
     * 跳转到商品列表页面
     * (windows)：优化前 1478.9
     *     加缓存优化后  2800
     * (linux) 290
     */
//    @GetMapping(value = "/list")
//    @AccessLimit
    @Deprecated
    public String toList(Model model, User user, HttpServletRequest req, HttpServletResponse resp) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        String html = (String) ops.get("goodsList");
        if (StringUtils.hasText(html)) {
            return html;
        }

        model.addAttribute("user", user);
        model.addAttribute("goodsList", goodsService.findGoodsVoList());
        WebContext context = new WebContext(req, resp, req.getServletContext(), req.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", context);
        ops.set("goodsList", html, 60, TimeUnit.SECONDS);
        return html;
    }

    /**
     * 跳转到商品列表页面
     */
    // @GetMapping(value = "/detail/{goodsId}/deprecated")
    @Deprecated
    @AccessLimit
    public String detail(Model model, User user, @PathVariable Long goodsId,
                         HttpServletRequest req, HttpServletResponse resp) {

        // 查找缓存
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        String goodsDetail = (String) ops.get("goodsDetail:" + goodsId);

        if (StringUtils.hasText(goodsDetail)) {
            return goodsDetail;
        }


        // 渲染页面
        model.addAttribute("user", user);
        GoodsVo goodsVo = goodsService.findGoodsVoById(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();

        int secKillStatus;
        int remainSeconds;
        //
        if (nowDate.before(startDate)) { // 秒杀未开始
            secKillStatus = 0;
            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if (nowDate.after(endDate)) { // 秒杀已经结束
            secKillStatus = 2;
            remainSeconds = -1;
        } else { // 秒杀进行中
            secKillStatus = 1;
            remainSeconds = 0;
        }

        model.addAttribute("secKillStatus", secKillStatus);
        model.addAttribute("goods", goodsVo);
        model.addAttribute("remainSeconds", remainSeconds);

        WebContext context = new WebContext(req, resp, req.getServletContext(), req.getLocale(), model.asMap());
        goodsDetail = thymeleafViewResolver.getTemplateEngine().process("goodsDetail",context);

        // 加入redis
        ops.set("goodsDetail:" + goodsId, goodsDetail, 60, TimeUnit.SECONDS);

        return goodsDetail;
    }

}
