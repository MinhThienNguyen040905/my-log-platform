import { Navbar } from '@/components/layout/Navbar';

export default function AppLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <>
      <Navbar />
      <main className='flex-1 pt-20 w-full'>{children}</main>
    </>
  );
}
