package com.mylog.insight.application;

import java.util.List;
import java.util.Map;

/**
 * DRAFT wording for thinking-pattern cards (tp-v1) until the reviewed knowledge articles TP_&lt;CODE&gt; exist
 * (THINKING_PATTERN_RECOGNITION_PLAN.md §4). Not reviewed by a mental-health professional yet: it must be
 * reviewed before any use with real users. Names habits of thought, never conditions or personality types.
 */
public final class ThinkingPatternTexts {
    public record Text(String name, String explanation, String weekTwo, String weekThree) {}

    public static final String WEEK_ONE = "Mỗi khi nhận ra mình đang nghĩ theo kiểu này, chỉ cần ghi lại, chưa cần thay đổi gì.";

    private static final Map<String, Text> TEXTS = Map.ofEntries(
            Map.entry("CATASTROPHIZING", new Text("đoán trước điều tệ nhất",
                    "Tâm trí nhảy thẳng tới kết cục xấu nhất, dù nó hiếm khi xảy ra. Rất nhiều người như vậy, nhất là khi đang chịu áp lực.",
                    "Khi nhận ra, hỏi mình: khả năng điều đó thật sự xảy ra là bao nhiêu phần trăm?",
                    "Viết ra kết quả có thể xảy ra nhất, không chỉ kết quả tệ nhất.")),
            Map.entry("ALL_OR_NOTHING", new Text("nghĩ kiểu \"tất cả hoặc không có gì\"",
                    "Mọi thứ bị chia thành hoàn hảo hoặc thất bại, không có mức ở giữa. Đây là lối tắt rất phổ biến của tâm trí.",
                    "Khi thấy mình nghĩ \"thất bại hoàn toàn\", thử chấm điểm việc đó trên thang 0 đến 10.",
                    "Đặt trước một mức \"tạm được\" cho một việc trong tuần và ghi nhận khi đạt mức đó.")),
            Map.entry("OVERGENERALIZATION", new Text("vơ đũa cả nắm",
                    "Từ một vài lần không như ý, tâm trí kết luận \"lúc nào cũng vậy\". Nhiều người làm thế để tự chuẩn bị cho điều xấu.",
                    "Khi viết \"lúc nào cũng\" hay \"chẳng bao giờ\", thử tìm một lần ngoại lệ.",
                    "Thay \"lúc nào cũng\" bằng một câu mô tả đúng sự việc lần này.")),
            Map.entry("MIND_READING", new Text("đoán ý người khác",
                    "Mình cho rằng đã biết người khác nghĩ gì, thường là điều không tốt về mình, dù họ chưa nói ra. Ai cũng có lúc như vậy.",
                    "Tách ra hai cột: điều mình biết chắc và điều mình đang đoán.",
                    "Chọn một lần an toàn để hỏi thẳng người kia, hoặc viết ra một cách hiểu khác về hành động của họ.")),
            Map.entry("SHOULD_STATEMENTS", new Text("áp lực \"phải\"",
                    "Những quy tắc cứng như \"phải luôn\", \"đáng lẽ\" khiến mình tự trách mỗi khi không đạt. Rất nhiều người mang những quy tắc này từ lâu.",
                    "Khi viết \"phải\" hay \"đáng lẽ\", hỏi mình: quy tắc này từ đâu ra, và nó có còn giúp mình không?",
                    "Thử đổi một câu \"mình phải\" thành \"mình muốn\" hoặc \"mình chọn\".")),
            Map.entry("SELF_LABELING", new Text("dán nhãn bản thân",
                    "Từ một hành động, mình gắn một cái nhãn cho cả con người mình. Đây là cách nói rất thường gặp khi thất vọng.",
                    "Khi nhận ra một cái nhãn, viết lại câu đó bằng cách mô tả hành động thay vì con người.",
                    "Ghi thêm một việc gần đây cho thấy mình không chỉ là cái nhãn đó.")),
            Map.entry("PERSONALIZATION", new Text("nhận hết lỗi về mình",
                    "Khi có chuyện không như ý, mình nghĩ ngay đó là lỗi của mình, dù còn nhiều yếu tố khác. Người chu đáo hay như vậy.",
                    "Liệt kê các yếu tố khác đã góp phần vào chuyện đó.",
                    "Chia \"phần trách nhiệm\" thành phần trăm cho từng yếu tố, kể cả phần của mình.")),
            Map.entry("DISCOUNTING_POSITIVE", new Text("gạt bỏ điều tốt",
                    "Điều tốt bị xem là may mắn hay chuyện nhỏ, nên khó thấy được mình đã làm được gì. Đây là thói quen phổ biến.",
                    "Mỗi ngày ghi một điều tốt mà không thêm chữ \"nhưng\".",
                    "Khi được khen, thử chỉ nói \"cảm ơn\" và để yên lời khen đó.")),
            Map.entry("EMOTIONAL_REASONING", new Text("lấy cảm xúc làm bằng chứng",
                    "Cảm thấy điều gì thì tin điều đó là thật. Cảm xúc là tín hiệu quan trọng, nhưng không phải lúc nào cũng là bằng chứng.",
                    "Khi thấy mình nghĩ \"thấy… nên chắc…\", hỏi: bằng chứng ngoài cảm giác là gì?",
                    "Viết cảm xúc và sự việc thành hai dòng riêng.")),
            Map.entry("RUMINATION", new Text("nghĩ đi nghĩ lại chuyện đã qua",
                    "Một chuyện đã qua cứ quay lại trong đầu mà không đi tới đâu. Rất nhiều người gặp vòng lặp này khi có điều chưa nguôi.",
                    "Đặt một \"giờ để nghĩ\" mười phút mỗi ngày; ngoài giờ đó thì ghi lại để dành cho giờ ấy.",
                    "Khi nghĩ lại, hỏi mình: có một bước nhỏ nào làm được ngay bây giờ không?")),
            Map.entry("WORRY_LOOP", new Text("lo lắng vòng tròn",
                    "Lo hết kịch bản này tới kịch bản khác về chuyện chưa xảy ra, biết là không giúp gì nhưng khó dừng. Ai cũng có lúc như vậy.",
                    "Chia các mối lo thành hai nhóm: điều mình làm được gì đó và điều không.",
                    "Với một điều làm được, viết ra bước đầu tiên và đặt lịch cho nó.")),
            Map.entry("AVOIDANCE", new Text("né tránh",
                    "Mình né một việc hay một người vì thấy khó chịu, dù biết sớm muộn cũng phải đối mặt. Né tránh giúp nhẹ lúc đầu nên rất dễ thành thói quen.",
                    "Ghi lại điều mình đang né và điều mình sợ nhất ở đó.",
                    "Làm hai phút đầu tiên của việc đang né, rồi được phép dừng.")),
            Map.entry("PROCRASTINATION_GUILT", new Text("trì hoãn rồi tự trách",
                    "Trì hoãn, rồi tự trách, rồi càng khó bắt đầu hơn. Vòng lặp này rất phổ biến và không nói lên con người bạn.",
                    "Chia việc đang hoãn thành bước nhỏ nhất có thể làm trong năm phút.",
                    "Khi lỡ trì hoãn, thử nói với mình bằng giọng tử tế như nói với một người bạn.")),
            Map.entry("PERFECTIONISM", new Text("cầu toàn",
                    "Tiêu chuẩn cao đến mức khó bắt đầu hoặc khó dừng lại. Nhiều người cầu toàn vì rất quan tâm tới việc mình làm.",
                    "Trước khi làm, viết ra thế nào là \"đủ tốt\" cho việc đó.",
                    "Nộp hoặc chia sẻ một việc khi nó vừa \"đủ tốt\", rồi ghi lại điều gì đã thật sự xảy ra.")),
            Map.entry("PEOPLE_PLEASING", new Text("khó từ chối",
                    "Mình đồng ý vì sợ người khác buồn hay mất lòng, kể cả khi điều đó đi ngược nhu cầu của mình. Người tốt bụng rất hay như vậy.",
                    "Khi được nhờ, cho mình thời gian: \"để mình xem lại rồi trả lời nhé\".",
                    "Tập một lời từ chối nhỏ, trong một tình huống an toàn.")),
            Map.entry("HARSH_SELF_CRITICISM", new Text("giọng nói bên trong khắt khe",
                    "Giọng nói bên trong mắng mỏ nặng nề mỗi khi mình chưa làm tốt. Rất nhiều người khắt khe với bản thân hơn với bất kỳ ai.",
                    "Khi nghe giọng đó, hỏi: mình sẽ nói gì với một người bạn trong tình huống này?",
                    "Viết lại một câu tự chê thành câu mình sẽ nói với người bạn ấy.")),
            Map.entry("SOCIAL_WITHDRAWAL", new Text("thu mình",
                    "Khi mệt, mình rút khỏi mọi người và càng lâu càng khó quay lại. Đây là phản ứng rất tự nhiên khi cạn năng lượng.",
                    "Mỗi tuần một liên lạc nhỏ: một tin nhắn hỏi thăm là đủ.",
                    "Hẹn một cuộc gặp ngắn, ở nơi quen thuộc, với một người mình thấy dễ chịu.")));

    private ThinkingPatternTexts() {}

    public static Text of(String code) {
        Text text = TEXTS.get(code);
        if (text == null) throw new IllegalArgumentException("Unknown thinking-pattern code");
        return text;
    }

    public static List<String> steps(String code) {
        Text text = of(code);
        return List.of(WEEK_ONE, text.weekTwo(), text.weekThree());
    }

    /** Card sentence: what was noticed, a normalising explanation, and the question that hands the decision back. */
    public static String narrative(String code, int entryCount, int weekCount) {
        Text text = of(code);
        return "Trong 30 ngày qua, có " + entryCount + " bài (trải trên " + weekCount + " tuần) bạn viết theo kiểu \""
                + text.name() + "\". " + text.explanation() + " Điều này có đúng với bạn không?";
    }
}
