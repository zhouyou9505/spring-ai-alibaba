import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "AG-UI Chat Demo",
  description: "A chat demo application using AG-UI protocol with Java backend",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
