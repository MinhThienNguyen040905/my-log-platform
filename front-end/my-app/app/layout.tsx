import type { Metadata } from "next";
import { Space_Grotesk, Plus_Jakarta_Sans, Playfair_Display } from "next/font/google";
import "./globals.css";
import { Navbar } from "@/components/layout/Navbar";
import { JournalProvider } from "@/lib/journal-context";

const spaceGrotesk = Space_Grotesk({
  variable: "--font-space-grotesk",
  subsets: ["latin", "vietnamese"],
  weight: ["500", "600", "700"],
});

const plusJakartaSans = Plus_Jakarta_Sans({
  variable: "--font-plus-jakarta",
  subsets: ["latin", "vietnamese"],
  weight: ["400", "500", "600", "700"],
});

const playfairDisplay = Playfair_Display({
  variable: "--font-playfair",
  subsets: ["latin", "vietnamese"],
  weight: ["400", "600", "700"],
});

export const metadata: Metadata = {
  title: "MyLog - AI Mental Wellness & Journaling",
  description: "Không gian nhật ký thông minh phong cách scrapbook hỗ trợ sức khỏe tinh thần và tự thấu cảm.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="vi"
      className={`${spaceGrotesk.variable} ${plusJakartaSans.variable} ${playfairDisplay.variable} h-full antialiased`}
    >
      <head>
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200"
        />
      </head>
      <body className="min-h-full flex flex-col bg-bg-canvas text-on-surface font-sans">
        <JournalProvider>
          <Navbar />
          <main className="flex-1 pt-20 w-full">{children}</main>
        </JournalProvider>
      </body>
    </html>
  );
}
