import { test, expect, type Page } from '@playwright/test';
import type { Goal } from '@/features/selfcare';

const TEST_USER_ID = '01950000-0000-7000-8000-000000000001';

async function setupAuthenticatedSession(page: Page) {
  await page.context().addCookies([
    {
      name: 'mylog_refresh',
      value: 'synthetic-refresh-token',
      domain: 'localhost',
      path: '/',
    },
  ]);

  await page.route('**/api/auth/session', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        userId: TEST_USER_ID,
        email: 'synthetic-user@example.test',
        displayName: 'Minh Anh',
        penName: 'Minh Anh',
        timezone: 'Asia/Ho_Chi_Minh',
        locale: 'vi',
        onboardingCompleted: true,
        onboardingGoals: ['SELF_CARE'],
        canChangePassword: true,
        version: 1,
      }),
    }),
  );

  await page.route('**/api/backend/dashboard**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        range: '30d',
        from: '2026-10-12',
        to: '2026-11-11',
        timezone: 'Asia/Ho_Chi_Minh',
        journalEntryCount: 5,
        currentJournalStreak: 3,
        moodSampleSize: 5,
        averageMood: 4,
        averageStress: 2,
        averageEnergy: 4,
        averageSleepHours: 7.5,
        emotionDistribution: [],
        topTopics: [],
        timeline: [
          { date: '2026-11-09', journalCount: 1, moodScore: 4, stressScore: 2, energyScore: 4, sleepHours: 7.5 },
          { date: '2026-11-10', journalCount: 1, moodScore: 4, stressScore: 2, energyScore: 4, sleepHours: 7.5 },
          { date: '2026-11-11', journalCount: 1, moodScore: 4, stressScore: 2, energyScore: 4, sleepHours: 7.5 },
        ],
      }),
    }),
  );
}

test.describe('Part 1 & Part 2 Web Features Verification (KT-03..KT-07)', () => {
  test.beforeEach(async ({}, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Run feature verification once on chromium.');
  });

  test('KT-03 & KT-06: InsightView Self-Care Goals/Habits checklist + Topic Recommendations (RAG)', async ({
    page,
  }) => {
    await setupAuthenticatedSession(page);

    const today = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Ho_Chi_Minh',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(new Date());

    let habitCompletedDates: string[] = [];
    const recordedCalls: string[] = [];

    const goalsState: Goal[] = [
      {
        id: '01950000-0000-7000-8000-000000000101',
        category: 'MINDFULNESS',
        title: 'Thư giãn buổi tối',
        description: 'Dành 10 phút tĩnh tâm trước khi ngủ',
        status: 'ACTIVE',
        startDate: null,
        targetDate: null,
        version: 1,
        createdAt: '2026-11-01T00:00:00Z',
        updatedAt: '2026-11-01T00:00:00Z',
        habits: [
          {
            id: '01950000-0000-7000-8000-000000000201',
            goalId: '01950000-0000-7000-8000-000000000101',
            title: 'Thở chậm 2 phút',
            targetValue: 2,
            unit: 'minutes',
            frequencyType: 'DAILY',
            daysOfWeek: [],
            timezone: 'Asia/Ho_Chi_Minh',
            toolCode: 'BREATHING_2MIN',
            status: 'ACTIVE',
            version: 1,
            streak: 1,
            completionCount: 1,
            completedDates: habitCompletedDates,
            moodAssociation: {
              completedDays: 7,
              otherDays: 7,
              averageMoodDifference: 0.6,
              interpretation: 'Những ngày thực hiện, tâm trạng trung bình cao hơn.',
            },
          },
        ],
      },
    ];

    await page.route('**/api/backend/insights**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ items: [], nextCursor: null }),
      }),
    );

    await page.route('**/api/backend/reports**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ items: [], nextCursor: null }),
      }),
    );

    await page.route('**/api/backend/self-care/goals', async (route) => {
      if (route.request().method() === 'GET') {
        goalsState[0].habits[0].completedDates = habitCompletedDates;
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(goalsState),
        });
      }
      const body = route.request().postDataJSON() as { category: string; title: string; description: string | null };
      recordedCalls.push(`POST /self-care/goals:${body.category}:${body.title}`);
      const created = {
        id: '01950000-0000-7000-8000-000000000102',
        category: body.category,
        title: body.title,
        description: body.description,
        status: 'ACTIVE',
        startDate: null,
        targetDate: null,
        version: 1,
        createdAt: '2026-11-11T00:00:00Z',
        updatedAt: '2026-11-11T00:00:00Z',
        habits: [],
      };
      goalsState.push(created);
      return route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(created),
      });
    });

    await page.route('**/api/backend/self-care/goals/*/habits', async (route) => {
      const body = route.request().postDataJSON() as { title: string; frequencyType: string; daysOfWeek?: number[] };
      recordedCalls.push(`POST /habits:${body.title}:${body.frequencyType}:${body.daysOfWeek ?? 'none'}`);
      const newHabit = {
        id: '01950000-0000-7000-8000-000000000202',
        goalId: goalsState[0].id,
        title: body.title,
        targetValue: 5,
        unit: 'minutes',
        frequencyType: body.frequencyType,
        daysOfWeek: [],
        timezone: 'Asia/Ho_Chi_Minh',
        toolCode: null,
        status: 'ACTIVE',
        version: 1,
        streak: 0,
        completionCount: 0,
        completedDates: [],
        moodAssociation: null,
      };
      goalsState[0].habits.push(newHabit);
      return route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(newHabit),
      });
    });

    await page.route(`**/api/backend/self-care/habits/*/completions/${today}`, async (route) => {
      const method = route.request().method();
      recordedCalls.push(`${method} /completions/${today}`);
      if (method === 'PUT') {
        habitCompletedDates = [today];
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ ...goalsState[0].habits[0], completedDates: habitCompletedDates }),
        });
      }
      habitCompletedDates = [];
      return route.fulfill({ status: 204 });
    });

    await page.route('**/api/backend/recommendations?topicCode=**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          topicCode: 'SELF_CARE',
          excerpts: [
            {
              text: 'Hít thở chậm giúp làm dịu hệ thần kinh tự chủ sau giờ làm việc.',
              citation: {
                itemId: '01950000-0000-7000-8000-000000000301',
                versionId: '01950000-0000-7000-8000-000000000302',
                version: 1,
                chunkId: '01950000-0000-7000-8000-000000000303',
                sourceName: 'Cẩm nang Tự chăm sóc',
                sourceUrl: 'https://example.org/self-care',
              },
            },
          ],
        }),
      }),
    );

    await page.route('**/api/backend/recommendations/questions', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          topicCode: 'SELF_CARE',
          questions: ['Khoảnh khắc nào hôm nay giúp bạn thấy dễ thở hơn?'],
          message: null,
        }),
      }),
    );

    await page.goto('/insight');
    await page.getByRole('button', { name: /Nhịp sống & Thói quen|Evidence/i }).click();

    // Verify standalone HabitTrackerCard (1..15 circular check grid) is inside "Nhịp sống & Thói quen" tab
    await expect(page.getByRole('columnheader', { name: /Thói quen/i })).toBeVisible();
    await expect(page.getByText('Tập thể dục', { exact: true })).toBeVisible();
    await expect(page.getByText('Học tập / Làm việc hiệu quả', { exact: true })).toBeVisible();
    await expect(page.getByText('Uống đủ nước', { exact: true })).toBeVisible();
    await expect(page.getByText('Ăn uống lành mạnh', { exact: true })).toBeVisible();

    const day1ExerciseBtn = page.getByRole('button', { name: 'Đánh dấu Tập thể dục ngày 1', exact: true });
    await expect(day1ExerciseBtn).toHaveAttribute('aria-pressed', 'false');
    await day1ExerciseBtn.click();
    await expect(
      page.getByRole('button', { name: 'Bỏ đánh dấu Tập thể dục ngày 1', exact: true }),
    ).toHaveAttribute(
      'aria-pressed',
      'true',
    );

    // Verify existing goal + habit + moodAssociation
    await expect(page.getByText('Thư giãn buổi tối')).toBeVisible();
    await expect(page.getByText('Những ngày thực hiện, tâm trạng trung bình cao hơn.')).toBeVisible();

    // Complete habit today (PUT) then Undo completion (DELETE)
    await page.getByRole('button', { name: 'Đánh dấu hoàn thành hôm nay cho Thở chậm 2 phút' }).click();
    await expect(
      page.getByRole('button', { name: 'Bỏ đánh dấu hoàn thành hôm nay cho Thở chậm 2 phút' }),
    ).toBeVisible();

    await page.getByRole('button', { name: 'Bỏ đánh dấu hoàn thành hôm nay cho Thở chậm 2 phút' }).click();
    await expect(
      page.getByRole('button', { name: 'Đánh dấu hoàn thành hôm nay cho Thở chậm 2 phút' }),
    ).toBeVisible();

    // Create new goal
    await page.getByRole('button', { name: 'Thêm mục tiêu' }).click();
    await page.getByLabel('Tên mục tiêu').fill('Ngủ trước 23h');
    await page.getByRole('button', { name: 'Lưu mục tiêu' }).click();
    await expect(page.getByText('Ngủ trước 23h')).toBeVisible();

    // Create new habit inside first goal
    await page.getByRole('button', { name: 'Thêm thói quen' }).first().click();
    await page.getByLabel('Tên thói quen mới').fill('Nghe tiếng mưa 5 phút');
    await page.getByRole('button', { name: 'Lưu thói quen' }).click();
    await expect(page.getByText('Nghe tiếng mưa 5 phút').first()).toBeVisible();

    expect(recordedCalls).toContain(`PUT /completions/${today}`);
    expect(recordedCalls).toContain(`DELETE /completions/${today}`);
    expect(recordedCalls).toContain('POST /self-care/goals:MINDFULNESS:Ngủ trước 23h');
    expect(recordedCalls).toContain('POST /habits:Nghe tiếng mưa 5 phút:DAILY:none');
  });

  test('KT-04 & KT-05: SettingsView User Feedback submission + Account Deletion request & cancel', async ({
    page,
  }) => {
    await setupAuthenticatedSession(page);

    const feedbackId = '01950000-0000-7000-8000-000000000401';
    const deletionId = '01950000-0000-7000-8000-000000000501';
    const actions: string[] = [];

    await page.route('**/api/me/consents', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
    );
    await page.route('**/api/me/sessions', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
    );
    await page.route('**/api/journal-entries**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ items: [], nextCursor: null }),
      }),
    );

    await page.route('**/api/backend/feedback', async (route) => {
      const body = route.request().postDataJSON() as { category: string; message: string };
      actions.push(`POST /feedback:${body.category}:${body.message}`);
      return route.fulfill({
        status: 202,
        contentType: 'application/json',
        body: JSON.stringify({ id: feedbackId }),
      });
    });

    await page.route(`**/api/backend/feedback/${feedbackId}`, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: feedbackId,
          userId: TEST_USER_ID,
          category: 'IDEA',
          status: 'OPEN',
          message: 'Thêm chế độ xem lịch theo quý',
          assignedTo: null,
          createdAt: '2026-11-11T10:00:00Z',
          updatedAt: '2026-11-11T10:00:00Z',
          resolvedAt: null,
          version: 1,
        }),
      }),
    );

    await page.route('**/api/backend/account-deletion-requests', async (route) => {
      const body = route.request().postDataJSON() as { password: string };
      actions.push(`POST /account-deletion-requests:${body.password.length > 0}`);
      return route.fulfill({
        status: 202,
        contentType: 'application/json',
        body: JSON.stringify({
          id: deletionId,
          status: 'PENDING',
          requestedAt: '2026-11-11T10:00:00Z',
          scheduledFor: '2026-11-18T10:00:00Z',
          completedAt: null,
        }),
      });
    });

    await page.route(`**/api/backend/account-deletion-requests/${deletionId}:cancel`, async (route) => {
      actions.push('POST /account-deletion-requests:cancel');
      return route.fulfill({ status: 204 });
    });

    await page.goto('/setting');

    // KT-04: Submit user feedback
    await page.getByLabel('Nội dung góp ý').fill('Thêm chế độ xem lịch theo quý');
    await page.getByRole('button', { name: 'Gửi góp ý' }).click();
    await expect(page.getByText(/Đã gửi góp ý thành công/)).toBeVisible();
    await expect(page.getByText('Mới tiếp nhận (OPEN)')).toBeVisible();

    // KT-05: Request account deletion & cancel within grace period
    const deletionSection = page.getByRole('region', { name: 'Xóa tài khoản vĩnh viễn' });
    await deletionSection.getByRole('checkbox').check();
    await deletionSection.getByLabel('Mật khẩu xác nhận xóa tài khoản').fill('ValidPassword123!');
    await deletionSection.getByRole('button', { name: 'Yêu cầu xóa tài khoản' }).click();
    await expect(deletionSection.getByText(deletionId)).toBeVisible();

    await deletionSection.getByLabel('Email xác thực yêu cầu xóa').fill('synthetic-user@example.test');
    await deletionSection.getByLabel('Mật khẩu quản lý yêu cầu xóa').fill('ValidPassword123!');
    await deletionSection.getByRole('button', { name: 'Hủy yêu cầu xóa' }).click();
    await expect(deletionSection.getByText('Đã hủy yêu cầu xóa tài khoản thành công.')).toBeVisible();

    expect(actions).toEqual([
      'POST /feedback:IDEA:Thêm chế độ xem lịch theo quý',
      'POST /account-deletion-requests:true',
      'POST /account-deletion-requests:cancel',
    ]);
  });

  test('KT-04, KT-06 & KT-07: AdminConsoleView User Metadata API, Feedback tab & Knowledge workflow tab', async ({
    page,
  }) => {
    await setupAuthenticatedSession(page);

    const targetUserId = '01950000-0000-7000-8000-000000000777';
    const feedbackId = '01950000-0000-7000-8000-000000000888';
    const knowledgeItemId = '01950000-0000-7000-8000-000000000999';
    const adminCalls: string[] = [];

    await page.route('**/api/backend/admin/dashboard', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalUsers: 12,
          activeUsers: 11,
          suspendedUsers: 1,
          pendingAiJobs: 0,
          failedAiJobs: 0,
          safetyEventsLast24h: 0,
        }),
      }),
    );

    await page.route('**/api/backend/admin/users?limit=20', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [
            {
              id: targetUserId,
              status: 'ACTIVE',
              createdAt: '2026-11-01T00:00:00Z',
              lastLoginAt: '2026-11-10T00:00:00Z',
              version: 2,
            },
          ],
          nextCursor: null,
        }),
      }),
    );

    await page.route(`**/api/backend/admin/users/${targetUserId}/metadata`, (route) => {
      adminCalls.push(`GET /admin/users/${targetUserId}/metadata`);
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: targetUserId,
          status: 'ACTIVE',
          createdAt: '2026-11-01T00:00:00Z',
          lastLoginAt: '2026-11-11T09:00:00Z',
          version: 5,
        }),
      });
    });

    await page.route('**/api/backend/admin/feedback**', (route) => {
      if (route.request().url().includes(`/admin/feedback/${feedbackId}`)) {
        if (route.request().method() === 'PATCH') {
          adminCalls.push(`PATCH /admin/feedback/${feedbackId}:${route.request().headers()['if-match']}`);
          return route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: feedbackId,
              userId: targetUserId,
              category: 'BUG',
              status: 'RESOLVED',
              message: 'Lỗi hiển thị lịch trên màn hình nhỏ',
              assignedTo: null,
              createdAt: '2026-11-11T08:00:00Z',
              updatedAt: '2026-11-11T09:00:00Z',
              resolvedAt: '2026-11-11T09:00:00Z',
              version: 2,
            }),
          });
        }
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: feedbackId,
            userId: targetUserId,
            category: 'BUG',
            status: 'OPEN',
            message: 'Lỗi hiển thị lịch trên màn hình nhỏ',
            assignedTo: null,
            createdAt: '2026-11-11T08:00:00Z',
            updatedAt: '2026-11-11T08:00:00Z',
            resolvedAt: null,
            version: 1,
          }),
        });
      }
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: feedbackId,
            userId: targetUserId,
            category: 'BUG',
            status: 'OPEN',
            message: 'Lỗi hiển thị lịch trên màn hình nhỏ',
            assignedTo: null,
            createdAt: '2026-11-11T08:00:00Z',
            updatedAt: '2026-11-11T08:00:00Z',
            resolvedAt: null,
            version: 1,
          },
        ]),
      });
    });

    await page.route('**/api/backend/admin/knowledge-items**', (route) => {
      const url = route.request().url();
      if (url.endsWith(':submit')) {
        adminCalls.push('POST :submit');
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            itemId: knowledgeItemId,
            versionId: '01950000-0000-7000-8000-000000000990',
            version: 1,
            slug: 'huong-dan-tho-cham',
            topicCode: 'SELF_CARE',
            locale: 'vi-VN',
            title: 'Hướng dẫn thở chậm',
            content: 'Nội dung thở chậm đã biên tập.',
            status: 'IN_REVIEW',
            checksum: 'abc123',
            chunkStrategyVersion: 'v1',
            effectiveFrom: null,
            effectiveTo: null,
          }),
        });
      }
      if (url.endsWith(':approve')) {
        adminCalls.push('POST :approve');
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            itemId: knowledgeItemId,
            versionId: '01950000-0000-7000-8000-000000000990',
            version: 1,
            slug: 'huong-dan-tho-cham',
            topicCode: 'SELF_CARE',
            locale: 'vi-VN',
            title: 'Hướng dẫn thở chậm',
            content: 'Nội dung thở chậm đã biên tập.',
            status: 'APPROVED',
            checksum: 'abc123',
            chunkStrategyVersion: 'v1',
            effectiveFrom: null,
            effectiveTo: null,
          }),
        });
      }
      adminCalls.push('POST /admin/knowledge-items');
      return route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          itemId: knowledgeItemId,
          versionId: '01950000-0000-7000-8000-000000000990',
          version: 1,
          slug: 'huong-dan-tho-cham',
          topicCode: 'SELF_CARE',
          locale: 'vi-VN',
          title: 'Hướng dẫn thở chậm',
          content: 'Nội dung thở chậm đã biên tập.',
          status: 'DRAFT',
          checksum: 'abc123',
          chunkStrategyVersion: 'v1',
          effectiveFrom: null,
          effectiveTo: null,
        }),
      });
    });

    await page.goto('/admin');

    // KT-07: Open User Metadata Drawer and verify it fetches fresh v5 metadata
    await page.getByRole('button', { name: 'Chi tiết' }).first().click();
    await expect(page.getByText('v5')).toBeVisible();
    await page.getByTitle('Đóng bảng chi tiết').click();

    // KT-04 (Admin): Switch to Feedback tab, inspect & update status to RESOLVED
    await page.getByRole('button', { name: 'Góp Ý Người Dùng' }).click();
    await expect(page.getByText('Lỗi hiển thị lịch trên màn hình nhỏ')).toBeVisible();
    await page.getByRole('button', { name: `Xem chi tiết góp ý ${feedbackId}` }).click();
    await page.getByLabel('Trạng thái xử lý góp ý').selectOption('RESOLVED');
    await page.getByRole('button', { name: 'Lưu cập nhật' }).click();
    await expect(page.getByText('Version: v2')).toBeVisible();

    // KT-06 (Admin): Switch to Knowledge Base tab, create item -> submit -> approve
    await page.getByRole('button', { name: 'Kho Kiến Thức' }).click();
    await page.getByLabel('Slug định danh').fill('huong-dan-tho-cham');
    await page.getByLabel('Tên nguồn trích dẫn').fill('Cẩm nang Tự chăm sóc');
    await page.getByLabel('Tiêu đề phiên bản').fill('Hướng dẫn thở chậm');
    await page.getByLabel('Nội dung kiến thức').fill('Nội dung thở chậm đã biên tập.');
    await page.getByRole('button', { name: 'Tạo bản nháp kiến thức' }).click();
    await expect(page.getByText('huong-dan-tho-cham · v1')).toBeVisible();

    await page.getByRole('button', { name: /Gửi duyệt/ }).click();
    await expect(page.getByText('IN_REVIEW', { exact: true })).toBeVisible();

    await page.getByRole('button', { name: /Phê duyệt/ }).click();
    await expect(page.getByText('APPROVED', { exact: true })).toBeVisible();

    expect(adminCalls).toEqual([
      `GET /admin/users/${targetUserId}/metadata`,
      `PATCH /admin/feedback/${feedbackId}:"1"`,
      'POST /admin/knowledge-items',
      'POST :submit',
      'POST :approve',
    ]);
  });
});
