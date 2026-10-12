import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const featureNames = ["admin", "auth", "calendar", "dashboard", "export", "feedback", "insights", "journal", "knowledge", "landing", "onboarding", "reporting", "selfcare", "user"];

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    files: ["features/**/*.{ts,tsx}", "components/**/*.{ts,tsx}", "providers/**/*.{ts,tsx}", "lib/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-imports": ["error", {
        patterns: [{ group: ["@/app/**"], message: "App is a composition layer; shared code and features must not import from it." }],
      }],
    },
  },
  {
    files: ["components/**/*.{ts,tsx}", "providers/**/*.{ts,tsx}", "lib/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-imports": ["error", {
        patterns: [
          { group: ["@/app/**"], message: "Shared code must not import from app." },
          { group: ["@/features/**"], message: "Shared code must not import feature business logic." },
        ],
      }],
    },
  },
  ...featureNames.map((name) => ({
    files: [`features/${name}/**/*.{ts,tsx}`],
    rules: {
      "no-restricted-imports": ["error", {
        patterns: [
          { group: ["@/app/**"], message: "Features must not import from app." },
          { group: [`@/features/${name}`, `@/features/${name}/**`], message: "Use relative imports inside the same feature instead of its public barrel." },
          ...(name === "user" ? [{ group: ["@/features/journal", "@/features/journal/**"], message: "Account state must not depend on journal state." }] : []),
        ],
      }],
    },
  })),
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;
