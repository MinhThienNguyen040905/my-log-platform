import { JournalEntry, WellnessGoal, DailyStat } from './types';

export const INITIAL_ENTRIES: JournalEntry[] = [
  {
    id: 'entry-1',
    title: 'Bảo vệ đề cương đồ án thành công: Trút bỏ cả gánh nặng 3 tuần qua!',
    content: 'Sáng nay lúc 9h bước vào phòng hội đồng với cảm giác tim đập thình thịch. Ba thầy cô trong hội đồng hỏi khá xoáy vào phần thuật toán phân tích cảm xúc và bảo mật nhật ký người dùng. May mắn là mình đã thức trắng 2 đêm trước chuẩn bị slide kỹ càng. Thầy chủ tịch khen ý tưởng scrapbook neo-brutalism rất sáng tạo và gần gũi. Bước ra khỏi phòng với một nụ cười thật tươi, cảm giác nhẹ bẫng như trút được một tảng đá đè nặng lên ngực cả tháng nay!',
    date: '2026-10-15',
    time: '14:30',
    mood: 'calm-joy',
    moodScore: 8.8,
    stressScore: 2.5,
    energyScore: 8.5,
    sleepHours: 7.5,
    tags: ['đồ án', 'đại học', 'chiến thắng', 'bình yên'],
    photoUrl: 'https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=600&auto=format&fit=crop&q=80',
    photoCaption: 'Khoảnh khắc chụp vội trước sảnh giảng đường sau khi bảo vệ xong',
    location: 'Đại học Bách Khoa',
    isFavorite: true,
    aiAnalysis: {
      sentiment: 'Tích cực cao & Cứu rỗi nội tâm',
      summary: 'Minh Anh đã vượt qua một cột mốc áp lực học thuật lớn. Mức độ căng thẳng giảm mạnh từ 8.5 xuống 2.5 sau khi nhận phản hồi tích cực.',
      reflectionPrompt: 'Khi bạn nhớ lại cảm giác nhẹ nhõm lúc bước ra khỏi phòng hội đồng, bài học lớn nhất về lòng kiên nhẫn với chính mình là gì?',
      mindfulAction: 'Hãy tự thưởng cho bản thân một buổi tối nghỉ ngơi hoàn toàn, không mở laptop công việc.',
      emotions: [
        { label: 'Bình an & Hân hoan', percentage: 65, color: '#70E000', description: 'Cảm giác giải tỏa và tự hào về nỗ lực cá nhân' },
        { label: 'Năng lượng hy vọng', percentage: 25, color: '#FFD166', description: 'Sẵn sàng cho các giai đoạn tiếp theo' },
        { label: 'Dư chấn hồi hộp', percentage: 10, color: '#FF6B6B', description: 'Cơ thể vẫn còn chút mệt mỏi từ 2 đêm thức khuya' }
      ],
      entities: [
        { name: 'Hội đồng đề cương', category: 'công việc', sentiment: 'positive' },
        { name: 'Thầy chủ tịch hội đồng', category: 'người', sentiment: 'positive' },
        { name: 'Thuật toán cảm xúc', category: 'công việc', sentiment: 'neutral' }
      ]
    }
  },
  {
    id: 'entry-2',
    title: '5km quanh bờ hồ và tách latte yến mạch ấm nóng',
    content: 'Dậy sớm lúc 5:45 sáng khi thành phố còn mờ sương. Không khí lành lạnh của mùa thu làm tâm trí mình tỉnh táo tức thì. Chạy chậm rãi dọc bờ hồ, ngắm nhìn các cô chú tập dưỡng sinh và những cụ già đọc báo bên ghế đá. Ghé quán quen gọi một ly oat latte nóng ít đường. Đôi khi hạnh phúc chỉ đơn giản là hít thật sâu mùi lá cây ẩm ướt sau cơn mưa đêm.',
    date: '2026-10-14',
    time: '07:45',
    mood: 'hope-energy',
    moodScore: 8.2,
    stressScore: 2.0,
    energyScore: 9.0,
    sleepHours: 8.0,
    tags: ['chạy bộ', 'sáng sớm', 'cà phê', 'thiên nhiên'],
    photoUrl: 'https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=600&auto=format&fit=crop&q=80',
    photoCaption: 'Tách latte yến mạch và góc phố sớm',
    location: 'Hồ Tây, Hà Nội',
    isFavorite: false,
    aiAnalysis: {
      sentiment: 'An nhiên & Tràn đầy sinh lực',
      summary: 'Hoạt động thể chất ngoài trời kết hợp với nhịp điệu tĩnh lặng của buổi sáng đã phục hồi năng lượng tinh thần tối đa.',
      reflectionPrompt: 'Bạn cảm nhận rõ nhất sự gắn kết với cơ thể và hơi thở của mình ở đoạn đường chạy nào?',
      mindfulAction: 'Duy trì thói quen thức dậy cùng ánh bình minh ít nhất 3 ngày trong tuần này.',
      emotions: [
        { label: 'Năng lượng tươi mới', percentage: 70, color: '#FFD166', description: 'Cảm giác tươi mát từ vận động' },
        { label: 'Thư thái nội tại', percentage: 30, color: '#70E000', description: 'Tĩnh tâm bên tách cà phê' }
      ],
      entities: [
        { name: 'Hồ Tây', category: 'địa điểm', sentiment: 'positive' },
        { name: 'Chạy bộ 5km', category: 'hoạt động', sentiment: 'positive' },
        { name: 'Latte yến mạch', category: 'hoạt động', sentiment: 'positive' }
      ]
    }
  },
  {
    id: 'entry-3',
    title: 'Chỉnh sửa chương 3: Đôi khi phải buông kỳ vọng hoàn hảo',
    content: 'Ngồi đối diện với bản thảo hơn 40 trang mà cảm thấy bế tắc. Có những đoạn viết cảm giác rất gượng gạo nhưng xóa đi thì tiếc công sức. Tự nhắc nhở bản thân rằng bản nháp đầu tiên luôn lộn xộn. Sau 2 tiếng đấu tranh, mình quyết định tái cấu trúc lại từ đầu và cắt bỏ 8 trang lan man. Thấy nhẹ nhõm hơn hẳn.',
    date: '2026-10-12',
    time: '21:15',
    mood: 'sadness-reflect',
    moodScore: 6.0,
    stressScore: 5.5,
    energyScore: 5.0,
    sleepHours: 6.5,
    tags: ['viết lách', 'suy ngẫm', 'buông bỏ', 'học tập'],
    location: 'Thư viện trung tâm',
    isFavorite: false,
    aiAnalysis: {
      sentiment: 'Tự vấn & Chấp nhận sự dang dở',
      summary: 'Xu hướng cầu toàn đã tạo ra sự căng thẳng ngầm. Tuy nhiên, hành động dũng cảm cắt bỏ nội dung thừa cho thấy tư duy trưởng thành.',
      reflectionPrompt: 'Điều gì khiến bạn khó từ bỏ những phần việc không còn phục vụ mục tiêu ban đầu?',
      mindfulAction: 'Thực hành bài tập thở 4-7-8 trước khi đi ngủ để hạ bớt suy nghĩ luẩn quẩn.',
      emotions: [
        { label: 'Trầm tư suy xét', percentage: 55, color: '#4D96FF', description: 'Đang tự đối thoại với những kỳ vọng của bản thân' },
        { label: 'Căng thẳng nhẹ', percentage: 30, color: '#FF6B6B', description: 'Áp lực thời hạn hoàn thành' },
        { label: 'Chấp nhận thực tế', percentage: 15, color: '#70E000', description: 'Nhận thức được giá trị của việc làm lại' }
      ],
      entities: [
        { name: 'Chương 3 bản thảo', category: 'công việc', sentiment: 'neutral' },
        { name: 'Kỳ vọng hoàn hảo', category: 'cảm xúc', sentiment: 'negative' }
      ]
    }
  }
];

export const MOCK_GOALS: WellnessGoal[] = [
  {
    id: 'goal-1',
    title: 'Ngủ đủ 7.5 tiếng mỗi đêm',
    description: 'Tắt màn hình điện thoại trước 23h, dùng đèn ngủ vàng ấm',
    category: 'sleep',
    targetDays: 7,
    completedDays: 5,
    unit: 'ngày',
    completed: false
  },
  {
    id: 'goal-2',
    title: 'Viết nhật ký phản chiếu mỗi tối',
    description: 'Dành 10 phút ghi lại 3 điều biết ơn và 1 cảm xúc trọng tâm',
    category: 'mindfulness',
    targetDays: 7,
    completedDays: 7,
    unit: 'ngày',
    completed: true
  },
  {
    id: 'goal-3',
    title: 'Đi bộ hoặc chạy bộ 30 phút',
    description: 'Tiếp xúc với ánh sáng mặt trời tự nhiên buổi sáng',
    category: 'exercise',
    targetDays: 4,
    completedDays: 3,
    unit: 'buổi',
    completed: false
  },
  {
    id: 'goal-4',
    title: 'Trò chuyện chân thành với bạn thân',
    description: 'Một cuộc gọi không vội vã để chia sẻ và lắng nghe',
    category: 'social',
    targetDays: 2,
    completedDays: 2,
    unit: 'lần',
    completed: true
  }
];

export const MOCK_14_DAYS_STATS: DailyStat[] = [
  { date: '02/10', dayName: 'T6', moodScore: 6.2, stressScore: 6.8, sleepHours: 6.0 },
  { date: '03/10', dayName: 'T7', moodScore: 7.0, stressScore: 5.0, sleepHours: 7.5 },
  { date: '04/10', dayName: 'CN', moodScore: 7.8, stressScore: 3.5, sleepHours: 8.0 },
  { date: '05/10', dayName: 'T2', moodScore: 6.5, stressScore: 6.0, sleepHours: 6.5 },
  { date: '06/10', dayName: 'T3', moodScore: 5.8, stressScore: 7.5, sleepHours: 5.5 },
  { date: '07/10', dayName: 'T4', moodScore: 6.0, stressScore: 7.0, sleepHours: 6.0 },
  { date: '08/10', dayName: 'T5', moodScore: 6.8, stressScore: 5.8, sleepHours: 6.8 },
  { date: '09/10', dayName: 'T6', moodScore: 7.2, stressScore: 4.5, sleepHours: 7.0 },
  { date: '10/10', dayName: 'T7', moodScore: 8.0, stressScore: 3.0, sleepHours: 8.0 },
  { date: '11/10', dayName: 'CN', moodScore: 7.5, stressScore: 3.8, sleepHours: 7.8 },
  { date: '12/10', dayName: 'T2', moodScore: 6.0, stressScore: 5.5, sleepHours: 6.5 },
  { date: '13/10', dayName: 'T3', moodScore: 7.0, stressScore: 4.2, sleepHours: 7.0 },
  { date: '14/10', dayName: 'T4', moodScore: 8.2, stressScore: 2.0, sleepHours: 8.0 },
  { date: '15/10', dayName: 'T5', moodScore: 8.8, stressScore: 2.5, sleepHours: 7.5 }
];
