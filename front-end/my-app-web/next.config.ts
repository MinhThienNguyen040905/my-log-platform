import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'res.cloudinary.com',
      },
      {
        protocol: 'https',
        hostname: 'images.unsplash.com',
      },
    ],
  },
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: [
          { key: 'Cross-Origin-Opener-Policy', value: 'same-origin-allow-popups' },
        ],
      },
      {
        source: '/auth/verify-email',
        headers: [
          { key: 'Referrer-Policy', value: 'no-referrer' },
          { key: 'Cache-Control', value: 'no-store' },
        ],
      },
      {
        source: '/auth/reset-password',
        headers: [
          { key: 'Referrer-Policy', value: 'no-referrer' },
          { key: 'Cache-Control', value: 'no-store' },
        ],
      },
    ];
  },
  async rewrites() {
    return [
      // With local asset storage (Cloudinary off) the backend returns signed image URLs as backend paths
      // (`/api/v1/journal-assets/raw/{id}?expires=&sig=`). In the browser those resolve against the web origin,
      // so route them to the existing signed-image proxy; the query string (expires, sig) is kept.
      { source: '/api/v1/journal-assets/raw/:id', destination: '/api/journal-assets/raw/:id' },
    ];
  },
};

export default nextConfig;
