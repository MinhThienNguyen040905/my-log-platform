'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getCheckin, putCheckin, type CheckinInput } from '../api/checkin';

/** Check-in của một ngày; lưu xong thì làm mới dashboard và insight vì chúng tính từ check-in. */
export function useDailyCheckin(userId: string, localDate: string) {
  const queryClient = useQueryClient();
  const queryKey = ['checkin', userId, localDate] as const;
  const query = useQuery({ queryKey, queryFn: () => getCheckin(localDate) });
  const mutation = useMutation({
    mutationFn: (input: CheckinInput) => putCheckin(localDate, input),
    // Mặc định React Query tạm hoãn mutation khi offline (nút treo ở "Đang lưu..."); ở đây báo lỗi
    // ngay để người dùng biết và thử lại — dữ liệu đang nhập vẫn được giữ trong form.
    networkMode: 'always',
    onSuccess: (saved) => {
      queryClient.setQueryData(queryKey, saved);
      void queryClient.invalidateQueries({ queryKey: ['dashboard', userId] });
      void queryClient.invalidateQueries({ queryKey: ['insights', userId] });
    },
  });
  return { query, mutation };
}
