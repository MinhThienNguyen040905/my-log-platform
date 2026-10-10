import { accountProxy } from '@/features/user/api/proxy';

export function GET(request: Request) { return accountProxy(request, '/me/sessions'); }
export function DELETE(request: Request) {
  return new URL(request.url).searchParams.get('exceptCurrent') === 'true'
    ? accountProxy(request, '/me/sessions?exceptCurrent=true')
    : new Response(null, { status: 400 });
}
