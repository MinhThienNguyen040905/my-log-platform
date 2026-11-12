#!/usr/bin/env node
// Seed a synthetic local test account with ~N days of journal entries, tags and check-ins so the
// dashboard / insight screens have data to show. Local development only: every request goes through
// the public API (encryption, safety screening and quotas behave exactly as for a real user).
//
// Usage (from repo root, backend running with profile `local`, Mailpit on :8025):
//   node backend/scripts/seed-demo-journal.mjs                       # create a new test account + seed 120 days
//   node backend/scripts/seed-demo-journal.mjs --days 90 --seed 7    # other length / scenario
//   node backend/scripts/seed-demo-journal.mjs --email a@mylog.test --password '...'   # seed an existing account
//   node backend/scripts/seed-demo-journal.mjs --email ... --password ... --reset      # delete its entries first
//   node backend/scripts/seed-demo-journal.mjs --email ... --password ... --rewrite-content
//        # also replace short bodies of existing entries with ~200-word synthetic text
//   node backend/scripts/seed-demo-journal.mjs --email ... --password ... --analyze-only
//        # grant AI_PROCESSING (synthetic account only), synthetic emotions for the last 30 days,
//        # then POST /insights:refresh and print the pattern insights (--analyze does it after seeding);
//        # add --reseed-analysis to replace earlier synthetic analyses after the seeder rules change
//
// Env: MYLOG_API_URL (default http://localhost:8080), MAILPIT_URL (default http://localhost:8025).
// All content is synthetic. Never point this at staging/prod or at a real person's account.

import { randomBytes, randomUUID } from 'node:crypto';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

// ---------- args ----------
const args = parseArgs(process.argv.slice(2));
const API = (process.env.MYLOG_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');
const MAILPIT = (process.env.MAILPIT_URL ?? 'http://localhost:8025').replace(/\/$/, '');
const DAYS = clampInt(args.days ?? 120, 7, 365);
const SEED = clampInt(args.seed ?? 42, 0, 2 ** 31 - 1);
const TZ = 'Asia/Ho_Chi_Minh'; // UTC+7, no DST: local times are converted with a fixed offset below.
const TZ_OFFSET_HOURS = 7;
const TARGET_WORDS = 200;        // evening entries
const TARGET_WORDS_MORNING = 110; // short morning notes
const SHORT_WORDS = 150;         // --rewrite-content: bodies below this are replaced

for (const url of [API, MAILPIT]) {
  const host = new URL(url).hostname;
  if (!['localhost', '127.0.0.1', '::1'].includes(host) && !args['allow-remote']) {
    fail(`Từ chối chạy với ${url}: script chỉ dành cho môi trường local.`);
  }
}

// ---------- deterministic randomness ----------
let rngState = SEED >>> 0;
function rand() { // mulberry32
  rngState = (rngState + 0x6d2b79f5) >>> 0;
  let t = rngState;
  t = Math.imul(t ^ (t >>> 15), t | 1);
  t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
}
const pick = (list) => list[Math.floor(rand() * list.length)];
const noise = (amp) => (rand() * 2 - 1) * amp;
const round1 = (v) => Math.round(v * 10) / 10;
const clampScore = (v) => round1(Math.min(10, Math.max(1, v)));

// ---------- synthetic content ----------
const TAGS = {
  work: { name: 'Công việc', color: '#5B7DB1' },
  family: { name: 'Gia đình', color: '#E0A458' },
  health: { name: 'Sức khỏe', color: '#6BAA75' },
  study: { name: 'Học tập', color: '#8E6CB5' },
  friends: { name: 'Bạn bè', color: '#D9707A' },
  rest: { name: 'Nghỉ ngơi', color: '#4FA3A5' },
};

const OPENINGS = [
  'Trời hôm nay âm u từ sáng, đến chiều thì có một cơn mưa ngắn làm không khí dịu hẳn đi.',
  'Sáng nay trời nắng nhẹ, mình đi làm sớm hơn mọi khi một chút nên đường còn khá vắng.',
  'Hôm nay là một ngày khá dài, đến tận bây giờ mình mới có thời gian ngồi xuống viết vài dòng.',
  'Mình bắt đầu ngày mới bằng một ly trà nóng và vài phút đứng nhìn ra cửa sổ.',
  'Buổi sáng mình dậy muộn hơn dự định nên mọi thứ có hơi vội vàng lúc đầu.',
  'Thời tiết mấy hôm nay thay đổi thất thường, sáng se lạnh mà trưa lại oi bức.',
  'Tối nay nhà yên tĩnh, mình bật một bản nhạc nhẹ rồi mở nhật ký ra viết.',
  'Một ngày trôi qua nhanh đến mức mình phải nhìn lại lịch mới nhớ hết những việc đã làm.',
  'Gió hôm nay thổi mạnh, đi ngoài đường thấy mát nhưng cũng hơi bụi.',
  'Mình ngồi viết ở bàn làm việc quen thuộc, bên cạnh là cốc nước và cuốn sổ ghi việc.',
];

const THEMES = {
  workBusy: {
    tags: ['work'],
    titles: ['Một ngày nhiều deadline', 'Cuộc họp kéo dài', 'Tuần làm việc dồn dập', 'Hoàn thành báo cáo', 'Guồng quay công việc'],
    lines: [
      'Hôm nay có ba cuộc họp liên tiếp nên gần như không có khoảng trống nào để tập trung làm việc riêng.',
      'Mình mất khá nhiều thời gian để sửa lại phần báo cáo theo góp ý của trưởng nhóm, có chỗ phải viết lại từ đầu.',
      'Danh sách việc cần làm dài hơn mình nghĩ, nên mình chia nhỏ ra từng phần để dễ bắt đầu hơn.',
      'Buổi chiều mình thấy hơi căng vì deadline được dời lên sớm hơn hai ngày mà khối lượng vẫn giữ nguyên.',
      'Có một đồng nghiệp hỏi mình về một lỗi trong hệ thống, hai đứa ngồi dò gần một tiếng mới tìm ra nguyên nhân.',
      'Mình nhận ra mình hay nhận thêm việc mà không cân nhắc thời gian, đây là điều mình muốn điều chỉnh.',
      'Giờ nghỉ trưa mình vẫn mở máy kiểm tra email, ăn vội nên đến chiều thấy hơi mệt.',
      'Cuối ngày mình hoàn thành được phần quan trọng nhất, dù vẫn còn vài việc nhỏ phải để sang ngày mai.',
      'Trưởng nhóm có khen phần trình bày của mình trong buổi họp, điều đó làm mình thấy công sức được ghi nhận.',
      'Mình thử tắt thông báo trong một tiếng để làm việc sâu, và thấy hiệu quả hơn hẳn so với buổi sáng.',
    ],
  },
  goodDay: {
    tags: ['rest'],
    titles: ['Một ngày nhẹ nhàng', 'Buổi sáng dễ chịu', 'Điều nhỏ làm mình vui', 'Cuối tuần thong thả', 'Chậm lại một chút'],
    lines: [
      'Sáng nay mình dậy sớm, pha một ly cà phê và ngồi ngoài ban công nghe tiếng chim một lúc lâu.',
      'Mình đi dạo quanh hồ gần nhà, trời mát, có gió nhẹ và nhiều người đang tập thể dục.',
      'Hôm nay mình đọc xong một cuốn sách đã để dang dở khá lâu, phần kết làm mình suy nghĩ khá nhiều.',
      'Buổi trưa mình nấu một món mới học trên mạng, lần đầu làm nhưng kết quả khá hài lòng.',
      'Mình sắp xếp lại góc làm việc, bỏ bớt những thứ không dùng đến và thấy không gian gọn gàng hơn.',
      'Chiều mình ghé một quán cà phê nhỏ, ngồi viết vài dòng và ngắm người qua lại.',
      'Mình tưới cây và để ý thấy chậu húng quế đã ra thêm nhiều lá non.',
      'Không có kế hoạch gì đặc biệt, nhưng chính sự thong thả đó lại làm mình thấy dễ chịu.',
      'Mình xem lại vài tấm ảnh cũ trong điện thoại và nhớ ra nhiều kỷ niệm vui đã gần như quên mất.',
    ],
  },
  tired: {
    tags: ['health'],
    titles: ['Hơi mệt', 'Thiếu ngủ', 'Ngày uể oải', 'Cần nghỉ sớm', 'Năng lượng thấp'],
    lines: [
      'Tối qua mình ngủ muộn vì xem điện thoại quá lâu, nên cả ngày hôm nay thấy uể oải và nặng đầu.',
      'Mình uống nhiều cà phê hơn bình thường để tỉnh táo, nhưng đến chiều thì lại thấy bồn chồn.',
      'Đầu óc không tập trung lắm, đọc một đoạn tài liệu mà phải đọc lại hai ba lần mới hiểu.',
      'Mình nhận ra khi ngủ ít thì mình dễ cáu hơn với những chuyện nhỏ, kể cả với người thân.',
      'Buổi trưa mình chợp mắt được mười lăm phút, sau đó thấy đỡ hơn một chút.',
      'Mình hủy một buổi hẹn không quá quan trọng để có thêm thời gian nghỉ ngơi.',
      'Cơ thể có vẻ đang nhắc mình rằng nhịp sinh hoạt mấy hôm nay chưa ổn lắm.',
      'Mình quyết định tối nay sẽ tắt điện thoại trước mười giờ và để nó ngoài phòng ngủ.',
    ],
  },
  family: {
    tags: ['family'],
    titles: ['Bữa cơm gia đình', 'Gọi điện cho mẹ', 'Cuối tuần ở nhà', 'Chuyện trong nhà', 'Những người thân'],
    lines: [
      'Cả nhà ăn tối cùng nhau, lâu rồi mới có dịp ngồi nói chuyện lâu như vậy mà không ai cầm điện thoại.',
      'Mình gọi điện cho mẹ, mẹ kể về khu vườn mới trồng thêm vài chậu rau và hỏi khi nào mình về thăm.',
      'Có chút bất đồng nhỏ trong nhà về chuyện chi tiêu, nhưng sau đó mọi người đã ngồi nói chuyện lại với nhau.',
      'Mình phụ dọn dẹp nhà cửa cả buổi sáng, xong việc thấy không gian thoáng hơn hẳn.',
      'Em mình kể về chuyện ở trường, nghe em nói mình nhớ lại thời đi học của chính mình.',
      'Bố nhờ mình cài lại điện thoại, hai bố con ngồi loay hoay khá lâu nhưng cũng vui.',
      'Mình nhận ra đã lâu mình chưa thật sự lắng nghe người nhà mà thường chỉ trả lời cho xong.',
      'Buổi chiều cả nhà cùng xem một bộ phim cũ, ai cũng nhớ những đoạn thoại quen thuộc.',
    ],
  },
  friends: {
    tags: ['friends'],
    titles: ['Gặp lại bạn cũ', 'Cà phê cuối tuần', 'Một buổi tối vui', 'Tin nhắn bất ngờ', 'Những người bạn'],
    lines: [
      'Mình gặp lại một người bạn cũ từ thời đại học, hai đứa nói chuyện suốt cả buổi chiều mà không thấy chán.',
      'Nhóm bạn rủ nhau đi ăn lẩu, không khí rất vui, mọi người kể đủ thứ chuyện trên đời.',
      'Một người bạn nhắn tin hỏi thăm khi thấy mình ít đăng gì gần đây, mình thấy được quan tâm.',
      'Bạn mình đang chuẩn bị chuyển công việc, mình ngồi nghe bạn chia sẻ những băn khoăn của bạn ấy.',
      'Mình cười khá nhiều hôm nay, cảm giác nhẹ nhõm hơn hẳn so với mấy ngày trước.',
      'Tụi mình hẹn nhau mỗi tháng gặp một lần, hy vọng lần này sẽ giữ được lời hứa đó.',
      'Mình kể cho bạn nghe về áp lực công việc gần đây, bạn không khuyên gì nhiều mà chỉ lắng nghe.',
      'Trên đường về mình nghĩ rằng những cuộc gặp như vậy giúp mình nạp lại năng lượng khá nhiều.',
    ],
  },
  health: {
    tags: ['health'],
    titles: ['Chạy bộ buổi sáng', 'Tập thể dục', 'Ăn uống lành mạnh hơn', 'Đi bộ 30 phút', 'Chăm sóc cơ thể'],
    lines: [
      'Mình chạy bộ khoảng ba cây số quanh công viên, lúc đầu hơi đuối nhưng sau đó thấy người nhẹ nhõm hẳn.',
      'Hôm nay mình tập giãn cơ mười lăm phút trước khi ngủ, lưng đỡ mỏi hơn so với mọi hôm.',
      'Mình cố gắng uống đủ nước và ăn thêm rau trong bữa trưa thay vì gọi đồ ăn nhanh như thường lệ.',
      'Đi bộ sau bữa tối giúp mình thấy thư giãn và dễ ngủ hơn, mình muốn duy trì thói quen này.',
      'Mình thử một bài tập yoga ngắn trên mạng, có vài động tác khó nhưng cũng làm được.',
      'Mình để ý rằng những hôm vận động buổi sáng thì cả ngày tinh thần đều tốt hơn.',
      'Mình ghi lại số bước chân mỗi ngày, tuần này đã cải thiện hơn tuần trước một chút.',
      'Buổi tối mình nấu cháo yến mạch đơn giản, ăn nhẹ bụng và không bị đầy.',
    ],
  },
  study: {
    tags: ['study'],
    titles: ['Học thêm kỹ năng mới', 'Một buổi học hiệu quả', 'Ôn tập', 'Khóa học online', 'Từng bước nhỏ'],
    lines: [
      'Mình dành một tiếng để học khóa học online, cuối cùng cũng hiểu được phần mà tuần trước còn mơ hồ.',
      'Ghi chú lại những điều mới học bằng chính lời của mình giúp mình nhớ lâu hơn nhiều.',
      'Có một đoạn khá khó hiểu, mình đánh dấu lại để xem kỹ hơn vào cuối tuần.',
      'Mình thấy vui khi tự làm được bài tập mà trước đó còn lúng túng không biết bắt đầu từ đâu.',
      'Mình thử giải thích lại bài học cho một người bạn, và nhận ra chỗ nào mình chưa thật sự hiểu.',
      'Học xong mình đi bộ một vòng để đầu óc thư giãn trước khi quay lại ôn tiếp.',
      'Mình đặt mục tiêu mỗi ngày học ít nhất ba mươi phút, hôm nay đã làm được.',
      'Có lúc mình thấy tiến bộ chậm, nhưng nhìn lại ghi chú của tháng trước thì đã khác khá nhiều.',
    ],
  },
  lowPeriod: {
    tags: ['work', 'health'],
    titles: ['Một giai đoạn chậm lại', 'Thấy hơi chùng xuống', 'Ngày không như mong đợi', 'Cần thêm thời gian', 'Nặng nề một chút'],
    lines: [
      'Dạo này mình thấy hơi chùng xuống, làm việc gì cũng chậm hơn bình thường và dễ bỏ dở giữa chừng.',
      'Mình không có nhiều hứng thú với những việc vẫn thường thích như đọc sách hay nghe nhạc.',
      'Mình thử viết ra vài điều đang làm mình bận tâm, nhìn chúng trên giấy thấy rõ ràng hơn một chút.',
      'Công việc dồn lại từ tuần trước khiến mình thấy hơi quá tải và không biết nên bắt đầu từ đâu.',
      'Mình ít nói chuyện với mọi người hơn, phần vì mệt, phần vì không muốn làm phiền ai.',
      'Mình tự nhắc bản thân rằng giai đoạn nào rồi cũng sẽ qua, và không cần phải ổn ngay lập tức.',
      'Hôm nay mình cho phép bản thân nghỉ sớm và không ép mình phải hoàn thành hết danh sách việc.',
      'Mình nhắn tin cho một người bạn thân, chỉ nói chuyện vài câu nhưng cũng thấy bớt nặng nề.',
    ],
  },
  recovery: {
    tags: ['rest', 'health'],
    titles: ['Dần ổn hơn', 'Lấy lại nhịp', 'Một bước nhỏ', 'Thấy khá hơn', 'Nhịp sống trở lại'],
    lines: [
      'Mấy hôm nay mình thấy ổn hơn, ngủ đủ giấc hơn và sáng dậy không còn thấy nặng nề như trước.',
      'Mình quay lại thói quen đi bộ buổi sáng và thấy có thêm năng lượng cho cả ngày.',
      'Mình ghi lại ba điều tốt trong ngày, hóa ra cũng không khó tìm như mình tưởng.',
      'Mình thấy nhẹ nhõm khi hoàn thành được việc đã trì hoãn suốt hai tuần qua.',
      'Mình bắt đầu sắp xếp lại lịch làm việc để có khoảng nghỉ giữa các đầu việc lớn.',
      'Nói chuyện với người thân nhiều hơn giúp mình thấy được hỗ trợ và bớt cô đơn.',
      'Mình không muốn vội vàng, chỉ cần mỗi ngày tốt hơn hôm qua một chút là đủ.',
      'Hôm nay mình còn đủ sức nấu một bữa tối tử tế, điều mà tuần trước mình không làm nổi.',
    ],
  },
  morning: {
    tags: [],
    titles: ['Ghi chú buổi sáng', 'Ý định cho hôm nay', 'Bắt đầu ngày mới'],
    lines: [
      'Hôm nay mình muốn tập trung vào một việc quan trọng nhất và làm nó trước mười giờ sáng.',
      'Mình thức dậy, uống một cốc nước ấm và hít thở chậm vài phút trước khi cầm điện thoại.',
      'Mục tiêu nhỏ cho hôm nay là đi ngủ trước mười một giờ và không ăn khuya.',
      'Mình liệt kê ba việc cần làm, ba việc có thể dời lại và một điều mình mong chờ trong ngày.',
      'Trời còn mát, mình mở cửa sổ cho thoáng phòng rồi mới bắt đầu làm việc.',
      'Mình nhắc bản thân rằng không cần hoàn hảo, chỉ cần bắt đầu là được.',
    ],
  },
};

const REFLECTIONS = {
  up: [
    'Nhìn lại cả ngày, mình thấy biết ơn vì mọi thứ diễn ra khá suôn sẻ.',
    'Mình nhận ra những niềm vui nhỏ như vậy thật ra xuất hiện khá thường xuyên nếu mình để ý.',
    'Mình muốn ghi lại cảm giác dễ chịu hôm nay để những lúc mệt mỏi có thể đọc lại.',
    'Hôm nay mình thấy mình kiên nhẫn với bản thân hơn, và điều đó làm mọi việc nhẹ nhàng hơn.',
    'Mình thấy năng lượng tốt hơn hẳn khi được ngủ đủ và có thời gian cho bản thân.',
  ],
  neutral: [
    'Nhìn chung hôm nay không có gì quá đặc biệt, nhưng cũng là một ngày ổn.',
    'Mình nghĩ mình cần cân bằng lại giữa thời gian làm việc và nghỉ ngơi.',
    'Có lúc mình thấy hơi mất tập trung, nhưng mình đã kéo mình quay lại được.',
    'Mình đang học cách chấp nhận rằng không phải ngày nào cũng hiệu quả như nhau.',
    'Viết ra như thế này giúp mình sắp xếp lại suy nghĩ rõ ràng hơn.',
  ],
  down: [
    'Hôm nay không dễ dàng, nhưng mình vẫn đi qua được và đó cũng là một điều đáng ghi nhận.',
    'Mình đang cố gắng không trách bản thân quá nhiều vì những việc chưa làm được.',
    'Mình nghĩ mình cần thêm thời gian nghỉ ngơi và bớt đặt kỳ vọng quá cao.',
    'Cảm giác mệt mỏi vẫn còn, nhưng viết ra đây giúp mình thấy nhẹ hơn một chút.',
    'Mình nhắc mình rằng có những ngày như vậy là bình thường.',
  ],
};

const CLOSINGS = [
  'Ngày mai mình muốn dậy sớm hơn mười lăm phút để có thời gian ăn sáng tử tế.',
  'Điều mình biết ơn nhất hôm nay là có một bữa tối ngon và một chỗ yên tĩnh để viết.',
  'Tối nay mình sẽ đọc vài trang sách rồi đi ngủ sớm.',
  'Mình sẽ thử đi bộ mười phút sau bữa trưa ngày mai xem có giúp tỉnh táo hơn không.',
  'Mình muốn nhắn tin hỏi thăm một người bạn mà lâu rồi chưa liên lạc.',
  'Ngày mai mình sẽ bắt đầu bằng việc khó nhất trước khi mở email.',
  'Mình biết ơn vì cơ thể vẫn đủ khỏe để đi qua một ngày dài như hôm nay.',
  'Trước khi ngủ mình sẽ chuẩn bị sẵn đồ cho ngày mai để sáng không phải vội.',
];

const ACTIVITIES = {
  workBusy: ['work-overtime', 'meeting'],
  goodDay: ['reading', 'walking', 'cooking'],
  tired: ['nap'],
  family: ['family-time', 'housework'],
  friends: ['social', 'eating-out'],
  health: ['running', 'stretching', 'walking'],
  study: ['studying', 'reading'],
  lowPeriod: ['journaling', 'walking'],
  recovery: ['walking', 'meditation', 'journaling'],
};

const wordCount = (text) => text.split(/\s+/).filter(Boolean).length;

function tiptap(paragraphs) {
  return {
    type: 'doc',
    content: paragraphs.map((text) => ({ type: 'paragraph', content: [{ type: 'text', text }] })),
  };
}

function takeFrom(pool, used) {
  const free = pool.filter((s) => !used.has(s));
  if (!free.length) return null;
  const s = pick(free);
  used.add(s);
  return s;
}

// Builds a 3–4 paragraph body of roughly `target` words (Vietnamese words = space-separated syllables).
function composeBody(themeKey, tone, morning) {
  const target = morning ? TARGET_WORDS_MORNING : TARGET_WORDS;
  const used = new Set();
  const theme = THEMES[themeKey].lines;
  const reflections = REFLECTIONS[tone];
  const paragraphs = morning
    ? [[takeFrom(OPENINGS, used)], [takeFrom(theme, used), takeFrom(theme, used)], [takeFrom(theme, used)]]
    : [[takeFrom(OPENINGS, used), takeFrom(OPENINGS, used)],
      [takeFrom(theme, used), takeFrom(theme, used), takeFrom(theme, used), takeFrom(theme, used)],
      [takeFrom(reflections, used), takeFrom(reflections, used)],
      [takeFrom(CLOSINGS, used)]];
  const total = () => wordCount(paragraphs.flat().join(' '));
  // Grow toward the target: alternate extra theme sentences (paragraph 2) and reflections (paragraph 3).
  for (let i = 0; total() < target - 10 && i < 12; i++) {
    const into = morning || i % 2 === 0 ? 1 : 2;
    const s = takeFrom(into === 1 ? theme : reflections, used);
    if (s) paragraphs[into].push(s);
  }
  return paragraphs.map((p) => p.filter(Boolean).join(' ')).filter(Boolean);
}

const toneOf = (mood) => (mood >= 7 ? 'up' : mood >= 4.8 ? 'neutral' : 'down');

// ---------- scenario ----------
function localToday() {
  const now = new Date(Date.now() + TZ_OFFSET_HOURS * 3600_000);
  return { y: now.getUTCFullYear(), m: now.getUTCMonth(), d: now.getUTCDate() };
}
function dayInfo(daysAgo) {
  const t = localToday();
  const date = new Date(Date.UTC(t.y, t.m, t.d - daysAgo));
  return { date, iso: date.toISOString().slice(0, 10), dow: date.getUTCDay() }; // 0 = Sunday
}
function occurredAt(date, hour, minute) {
  return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate(),
    hour - TZ_OFFSET_HOURS, minute)).toISOString();
}

function buildPlan() {
  // Index 0 = today. Generate oldest -> newest so "previous night's sleep" flows forward.
  const lowStart = DAYS >= 60 ? 52 : -1; // 2-week low period ~7 weeks ago, then a recovery week
  const lowEnd = lowStart - 13;
  const recoveryEnd = lowEnd - 7;
  const gapDays = new Set();
  for (let i = DAYS - 1; i >= 16; i--) {
    if (rand() < 0.045) {
      const len = 2 + Math.floor(rand() * 3);
      for (let k = 0; k < len && i - k >= 16; k++) gapDays.add(i - k);
      i -= len;
    } else if (rand() < 0.07) gapDays.add(i);
  }

  const days = [];
  let prevSleep = 420;
  for (let i = DAYS - 1; i >= 0; i--) {
    const info = dayInfo(i);
    const weekend = info.dow === 0 || info.dow === 6;
    const inLow = i <= lowStart && i > lowEnd;
    const inRecovery = i <= lowEnd && i > recoveryEnd;

    let sleep = 430 + (weekend ? 45 : 0) + noise(40);
    if (rand() < (inLow ? 0.35 : 0.15)) sleep = 300 + noise(30); // short night
    sleep = Math.round(Math.min(600, Math.max(240, sleep)) / 5) * 5;

    const stressByDow = [-1.2, 2.0, 1.8, 1.5, 0.6, 0.2, -1.5][info.dow];
    let stress = 4.2 + stressByDow + (inLow ? 1.5 : 0) + (inRecovery ? 0.3 : 0) + noise(1.0);
    let mood = 6.6 + (weekend ? 0.8 : 0) - 0.35 * (stress - 4.5) + (prevSleep < 360 ? -1.3 : 0)
      + (inLow ? -2.3 : 0) + (inRecovery ? -0.6 : 0) + noise(0.8);
    let energy = 5.5 + (sleep - 420) / 60 + (inLow ? -1.5 : 0) + noise(0.9);
    stress = clampScore(stress); mood = clampScore(mood); energy = clampScore(energy);

    let theme;
    if (inLow) theme = 'lowPeriod';
    else if (inRecovery) theme = 'recovery';
    else if (prevSleep < 360 || sleep < 340) theme = 'tired';
    else if (!weekend && stress >= 6) theme = 'workBusy';
    else if (weekend) theme = pick(['goodDay', 'family', 'friends', 'health']);
    else theme = pick(['workBusy', 'study', 'health', 'goodDay', 'friends']);

    let moodCode = mood >= 7.5 ? 'happy' : mood >= 5.5 ? 'neutral' : 'sad';
    if (stress >= 7.5 && mood < 6) moodCode = 'angry';
    if (mood < 3) moodCode = 'very-bad';

    days.push({ daysAgo: i, ...info, sleep, prevSleep, stress, mood, energy, theme, moodCode,
      skipJournal: gapDays.has(i), extraMorning: !gapDays.has(i) && rand() < 0.15,
      checkin: rand() < (gapDays.has(i) ? 0.4 : 0.72) });
    prevSleep = sleep;
  }
  return days.reverse(); // newest first: the 7d/30d windows fill up before quota pauses
}

function entryRequest(day, morning) {
  const themeKey = morning ? 'morning' : day.theme;
  const theme = THEMES[themeKey];
  const hour = morning ? 6 + Math.floor(rand() * 3) : 19 + Math.floor(rand() * 4);
  const body = {
    title: pick(theme.titles),
    contentJson: tiptap(composeBody(themeKey, toneOf(day.mood), morning)),
    occurredAt: occurredAt(day.date, hour, Math.floor(rand() * 60)),
    timezone: TZ,
    moodCode: day.moodCode,
    moodScore: morning ? clampScore(day.mood + noise(0.6)) : day.mood,
    stressScore: morning ? clampScore(day.stress - 0.8) : day.stress,
    energyScore: day.energy,
    sleepMinutes: day.sleep,
    tagNames: [...new Set([...theme.tags, ...(rand() < 0.2 ? [pick(Object.keys(TAGS))] : [])])],
  };
  if (rand() < 0.1) delete body.energyScore;    // some entries leave fields empty
  if (morning || rand() < 0.08) delete body.sleepMinutes;
  return body;
}

// ---------- http ----------
const session = { accessToken: null, refreshToken: null };

async function http(method, path, { body, headers = {}, auth = true, okStatuses } = {}) {
  let refreshed = 0; // separate from the 429 wait loop: the access token can expire while waiting
  for (let attempt = 0; attempt < 40; attempt++) {
    const res = await fetch(API + path, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(auth && session.accessToken ? { Authorization: `Bearer ${session.accessToken}` } : {}),
        ...headers,
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (res.status === 429) {
      process.stdout.write(`\n  Đạt giới hạn ghi của API (60 lượt/15 phút). Chờ 60 giây rồi thử lại...`);
      await sleep(60_000);
      continue;
    }
    if (res.status === 401 && auth && session.refreshToken && refreshed < 2) {
      refreshed++;
      await refresh();
      continue;
    }
    const text = await res.text();
    const data = text ? safeJson(text) : null;
    if (!res.ok && !(okStatuses ?? []).includes(res.status)) {
      const detail = data && typeof data === 'object' ? (data.title ?? data.detail ?? '') : '';
      throw new Error(`${method} ${path} -> HTTP ${res.status} ${detail}`.trim());
    }
    return { status: res.status, data, etag: res.headers.get('etag') };
  }
  throw new Error(`${method} ${path}: vẫn bị giới hạn sau nhiều lần chờ.`);
}

async function refresh() {
  const { data } = await http('POST', '/api/v1/auth/refresh', { body: { refreshToken: session.refreshToken }, auth: false });
  session.accessToken = data.accessToken;
  session.refreshToken = data.refreshToken ?? session.refreshToken;
}

async function login(email, password) {
  const { data } = await http('POST', '/api/v1/auth/login',
    { body: { email, password, deviceName: 'seed-demo-journal' }, auth: false });
  session.accessToken = data.accessToken;
  session.refreshToken = data.refreshToken;
}

// ---------- account ----------
async function createAccount() {
  const stamp = new Date().toISOString().replace(/[-:T]/g, '').slice(0, 12);
  const email = `analytics.demo.${stamp}@mylog.test`;
  const password = `Demo-${randomBytes(12).toString('base64url')}`;
  await http('POST', '/api/v1/auth/register', {
    auth: false,
    body: { email, password, timezone: TZ, locale: 'vi', termsVersion: 'v1', privacyVersion: 'v1',
      acceptTerms: true, acceptPrivacy: true },
  });
  const code = await waitForVerificationCode(email);
  await http('POST', '/api/v1/auth/email-verifications:confirm-code', { auth: false, body: { email, code } });
  return { email, password };
}

async function waitForVerificationCode(email) {
  for (let i = 0; i < 30; i++) {
    try {
      const search = await fetch(`${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:"${email}"`)}`);
      if (search.ok) {
        const list = await search.json();
        const id = list.messages?.[0]?.ID;
        if (id) {
          const msg = await (await fetch(`${MAILPIT}/api/v1/message/${id}`)).json();
          const match = `${msg.Text ?? ''}\n${msg.HTML ?? ''}`.match(/\b(\d{8})\b/);
          if (match) return match[1];
        }
      }
    } catch { /* Mailpit not ready yet */ }
    await sleep(1000);
  }
  fail(`Không thấy email xác minh trong Mailpit (${MAILPIT}). Kiểm tra Mailpit đang chạy và backend dùng profile local.`);
}

// ---------- seeding ----------
async function ensureTags() {
  const { data: existing } = await http('GET', '/api/v1/journal-tags');
  const byName = new Map((existing ?? []).map((t) => [t.name.toLowerCase(), t.id]));
  const ids = {};
  for (const [key, tag] of Object.entries(TAGS)) {
    let id = byName.get(tag.name.toLowerCase());
    if (!id) id = (await http('POST', '/api/v1/journal-tags', { body: tag })).data.id;
    ids[key] = id;
  }
  return ids;
}

async function listEntries(from, to) {
  const items = [];
  let cursor = null;
  do {
    const qs = new URLSearchParams({ from, to, limit: '50', ...(cursor ? { cursor } : {}) });
    const { data } = await http('GET', `/api/v1/journal-entries?${qs}`);
    items.push(...data.items);
    cursor = data.nextCursor;
  } while (cursor);
  return items;
}

async function createEntry(body, tagIds) {
  const { tagNames, ...rest } = body;
  const { data } = await http('POST', '/api/v1/journal-entries', {
    headers: { 'Idempotency-Key': `seed-${randomUUID()}` },
    body: { ...rest, tagIds: tagNames.map((n) => tagIds[n]).filter(Boolean) },
  });
  return data;
}

function plainText(node) {
  if (!node || typeof node !== 'object') return '';
  if (node.type === 'text') return node.text ?? '';
  return (node.content ?? []).map(plainText).join(' ');
}

// Replace short bodies of entries created by an earlier run; scores, dates and tags stay as they are.
async function rewriteShortEntries(from, to) {
  const themeByTag = {
    [TAGS.work.name]: 'workBusy', [TAGS.family.name]: 'family', [TAGS.health.name]: 'health',
    [TAGS.study.name]: 'study', [TAGS.friends.name]: 'friends', [TAGS.rest.name]: 'goodDay',
  };
  const entries = await listEntries(from, to);
  let rewritten = 0;
  for (const summary of entries) {
    const { data: detail, etag } = await http('GET', `/api/v1/journal-entries/${summary.id}`);
    const localHour = (new Date(detail.occurredAt).getUTCHours() + TZ_OFFSET_HOURS) % 24;
    const morning = localHour < 12;
    if (wordCount(plainText(detail.contentJson)) >= (morning ? 80 : SHORT_WORDS)) continue;
    const mood = detail.moodScore == null ? 6 : Number(detail.moodScore);
    const tagTheme = (summary.tags ?? []).map((t) => themeByTag[t.name]).find(Boolean);
    const themeKey = morning ? 'morning' : mood < 4.5 ? 'lowPeriod' : tagTheme ?? 'goodDay';
    await http('PATCH', `/api/v1/journal-entries/${summary.id}`, {
      headers: { 'If-Match': etag },
      body: { title: pick(THEMES[themeKey].titles), contentJson: tiptap(composeBody(themeKey, toneOf(mood), morning)) },
    });
    rewritten++;
    process.stdout.write(`\r  Viết lại nội dung: ${rewritten} nhật ký (${summary.localDate})   `);
  }
  console.log('');
  return rewritten;
}

// Option 1a + pattern insights: synthetic emotions/topics for the last 30 days, then a refresh.
async function analyzeAndRefresh() {
  // Synthetic test account only: allow AI wording of insight sentences (facts only, never journal text).
  await http('PUT', '/api/v1/me/consents/AI_PROCESSING', { body: { documentVersion: 'v1', granted: true } });
  // --reseed-analysis replaces earlier synthetic (LOCAL_FAKE) analyses of the window; real ones are never touched.
  const { data: seeded } = await http('POST', `/api/v1/dev/analyses:seed${args['reseed-analysis'] ? '?reseed=true' : ''}`);
  console.log(`Phân tích mẫu: ${seeded.analyzed} bài (${seeded.from} → ${seeded.to}), bỏ qua ${seeded.skipped}.`);
  const { data: page } = await http('POST', '/api/v1/insights:refresh');
  const patterns = page.items.filter((i) => i.algorithmVersion?.startsWith('pattern-'));
  console.log(`Insight 30 ngày: ${patterns.length}`);
  for (const i of patterns) console.log(`  [${i.type} · ${i.generatedBy}] ${i.narrative}`);
}

async function main() {
  console.log(`API: ${API} | Mailpit: ${MAILPIT} | ${DAYS} ngày | seed ${SEED}`);
  const health = await fetch(`${API}/actuator/health`).catch(() => null);
  if (!health) fail(`Không kết nối được backend tại ${API}.`);

  let creds;
  if (args.email) {
    if (!args.password) fail('Cần --password khi dùng --email.');
    creds = { email: args.email, password: args.password };
  } else {
    console.log('Tạo tài khoản test qua đăng ký + mã xác minh từ Mailpit...');
    creds = await createAccount();
    const out = join(dirname(fileURLToPath(import.meta.url)), '..', 'scratch', 'seed-demo-account.txt');
    mkdirSync(dirname(out), { recursive: true });
    writeFileSync(out, `email=${creds.email}\npassword=${creds.password}\ncreated=${new Date().toISOString()}\n`);
    console.log(`\n  Tài khoản test đã sẵn sàng (lưu cả ở backend/scratch/seed-demo-account.txt, đã gitignore):`);
    console.log(`    email:    ${creds.email}`);
    console.log(`    password: ${creds.password}\n`);
  }
  await login(creds.email, creds.password);

  if (args['analyze-only']) { await analyzeAndRefresh(); return; }

  const plan = buildPlan();
  const from = plan[plan.length - 1].iso;
  const to = plan[0].iso;

  if (args.reset) {
    const existing = await listEntries(from, to);
    console.log(`--reset: xóa ${existing.length} nhật ký trong khoảng ${from} → ${to}...`);
    for (const e of existing) {
      await http('DELETE', `/api/v1/journal-entries/${e.id}`, { headers: { 'If-Match': `"${e.rowVersion}"` } });
    }
  }

  const tagIds = await ensureTags();
  const existingDates = new Set((await listEntries(from, to)).map((e) => e.localDate));

  let created = 0, checkins = 0, skipped = 0;
  const createdEntries = [];
  for (const [index, day] of plan.entries()) {
    if (day.checkin) {
      const acts = ACTIVITIES[day.theme] ?? [];
      await http('PUT', `/api/v1/check-ins/${day.iso}`, {
        body: {
          timezone: TZ, moodCode: day.moodCode, moodScore: day.mood, stressScore: day.stress,
          energyScore: day.energy, sleepMinutes: day.sleep,
          activities: acts.slice(0, 1 + Math.floor(rand() * acts.length)).map((code) => ({
            code, durationMinutes: 15 + Math.floor(rand() * 8) * 15, intensity: pick(['LOW', 'MODERATE', 'HIGH']),
          })),
        },
      });
      checkins++;
    }
    if (day.skipJournal) continue;
    if (existingDates.has(day.iso)) { skipped++; continue; }

    if (day.extraMorning) { await createEntry(entryRequest(day, true), tagIds); created++; }
    const entry = await createEntry(entryRequest(day, false), tagIds);
    createdEntries.push({ id: entry.id, mood: day.mood, daysAgo: day.daysAgo });
    created++;
    process.stdout.write(`\r  Ngày ${index + 1}/${plan.length} (${day.iso}) — đã tạo ${created} nhật ký, ${checkins} check-in   `);
  }
  console.log('');

  // A few favorites (best days) and two deleted entries, to exercise filters/edge cases.
  const favorites = [...createdEntries].sort((a, b) => b.mood - a.mood).slice(0, 5);
  for (const f of favorites) await http('PUT', `/api/v1/journal-entries/${f.id}/favorite`);
  const victims = createdEntries.filter((e) => e.daysAgo > 20).slice(-2);
  for (const v of victims) {
    const { etag } = await http('GET', `/api/v1/journal-entries/${v.id}`);
    await http('DELETE', `/api/v1/journal-entries/${v.id}`, { headers: { 'If-Match': etag } });
  }

  const rewritten = args['rewrite-content'] ? await rewriteShortEntries(from, to) : 0;

  console.log(`Xong: ${created} nhật ký mới (${skipped} ngày đã có sẵn bị bỏ qua), ${checkins} check-in, `
    + `${favorites.length} yêu thích, ${victims.length} bị xóa, ${rewritten} nhật ký được viết lại nội dung. `
    + `Khoảng ngày ${from} → ${to}.`);
  if (args.analyze) await analyzeAndRefresh();
  else console.log('Thêm --analyze để gán cảm xúc mẫu cho 30 ngày gần nhất và sinh insight.');
}

// ---------- utils ----------
function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (!a.startsWith('--')) continue;
    const key = a.slice(2);
    const next = argv[i + 1];
    if (next === undefined || next.startsWith('--')) out[key] = true;
    else { out[key] = next; i++; }
  }
  return out;
}
function clampInt(v, min, max) {
  const n = Number.parseInt(v, 10);
  if (!Number.isFinite(n)) fail(`Giá trị không hợp lệ: ${v}`);
  return Math.min(max, Math.max(min, n));
}
function safeJson(text) { try { return JSON.parse(text); } catch { return text; } }
function sleep(ms) { return new Promise((r) => setTimeout(r, ms)); }
function fail(message) { console.error(`Lỗi: ${message}`); process.exit(1); }

main().catch((err) => fail(err.message));
