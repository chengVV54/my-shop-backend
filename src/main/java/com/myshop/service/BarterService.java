package com.myshop.service;

import com.myshop.model.BarterRequestVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 换物申请业务逻辑。当前返回内存假数据，后续接入 MyBatis-Plus + MySQL。
 */
@Service
public class BarterService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private static final List<BarterRequestVO> MOCK_BARTERS = buildMockBarters();

    /**
     * 查询全部换物申请。
     *
     * @return 换物申请列表
     */
    public List<BarterRequestVO> listAll() {
        return new ArrayList<>(MOCK_BARTERS);
    }

    /**
     * 构建 2 条内存假数据。
     */
    private static List<BarterRequestVO> buildMockBarters() {
        String now = LocalDateTime.now().format(TIME_FORMATTER);

        // 用户 2 用「机械键盘」（物品 6）换用户 1 的「佳能 EOS R6」（物品 1），主动补 500 差价
        BarterRequestVO request1 = new BarterRequestVO(
                1L,
                2L,
                1L,
                6L,
                1L,
                "想用全新机械键盘换你的相机，我补 500 差价，可以详聊。",
                new BigDecimal("500.00"),
                "PENDING",
                now);

        // 用户 1 用「Switch OLED」（物品 5）换用户 2 的「三体全集」（物品 2），无需补差价
        BarterRequestVO request2 = new BarterRequestVO(
                2L,
                1L,
                2L,
                5L,
                2L,
                "用 Switch OLED 换你的三体全集，不用补差价，你看行吗？",
                new BigDecimal("0.00"),
                "ACCEPTED",
                now);

        return List.of(request1, request2);
    }
}
