import { EmotionType, EmotionDefinition } from '@/types';

export const EMOTION_TAXONOMY: Record<EmotionType, EmotionDefinition> = {
  JOY: { type: 'JOY', label: 'Hân hoan (Joy)', color: '#70E000', description: 'Cảm giác vui tươi, mãn nguyện' },
  CALM: { type: 'CALM', label: 'Bình an (Calm)', color: '#38B000', description: 'Tĩnh lặng, thư thái nội tâm' },
  HOPE: { type: 'HOPE', label: 'Hy vọng (Hope)', color: '#FFD166', description: 'Lạc quan, hướng về phía trước' },
  GRATITUDE: { type: 'GRATITUDE', label: 'Biết ơn (Gratitude)', color: '#06D6A0', description: 'Trân trọng những điều tốt đẹp' },
  EXCITEMENT: { type: 'EXCITEMENT', label: 'Hào hứng (Excitement)', color: '#FFB703', description: 'Tràn đầy sinh lực và hứng khởi' },
  ANXIETY: { type: 'ANXIETY', label: 'Lo âu (Anxiety)', color: '#FF6B6B', description: 'Bồn chồn, áp lực suy nghĩ' },
  SADNESS: { type: 'SADNESS', label: 'Buồn bã (Sadness)', color: '#4D96FF', description: 'Trầm tư, trống trải' },
  LONELINESS: { type: 'LONELINESS', label: 'Cô đơn (Loneliness)', color: '#9D4EDD', description: 'Cảm giác thiếu gắn kết' },
  FRUSTRATION: { type: 'FRUSTRATION', label: 'Bực bội (Frustration)', color: '#F72585', description: 'Ức chế, nghẽn trở ý muốn' },
  FEAR: { type: 'FEAR', label: 'Sợ hãi (Fear)', color: '#D90429', description: 'Bất an trước bất định' },
  ANGER: { type: 'ANGER', label: 'Giận dữ (Anger)', color: '#E63946', description: 'Căng thẳng phản ứng gay gắt' },
};

