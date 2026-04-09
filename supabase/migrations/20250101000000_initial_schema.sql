-- ============================================================
-- Bilbo Initial Database Schema
-- Supabase / PostgreSQL
-- ============================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ============================================================
-- Profiles
-- Extended user data beyond what Supabase Auth provides.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE,
    display_name TEXT,
    avatar_url TEXT,
    timezone TEXT NOT NULL DEFAULT 'UTC',
    onboarded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- ============================================================
-- App Usage Entries
-- Raw screen time records synced from mobile clients.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.app_usage_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    app_name TEXT NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    category TEXT NOT NULL DEFAULT 'other',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_app_usage_user_id ON public.app_usage_entries(user_id);
CREATE INDEX idx_app_usage_start_time ON public.app_usage_entries(start_time DESC);
CREATE INDEX idx_app_usage_package ON public.app_usage_entries(user_id, package_name);

ALTER TABLE public.app_usage_entries ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own usage data"
    ON public.app_usage_entries
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ============================================================
-- Wellness Goals
-- User-defined daily screen time limits and focus goals.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.wellness_goals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    type TEXT NOT NULL,
    target_apps JSONB NOT NULL DEFAULT '[]',
    daily_limit_minutes INTEGER NOT NULL DEFAULT 60,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_goals_user_id ON public.wellness_goals(user_id);
CREATE INDEX idx_goals_is_active ON public.wellness_goals(user_id, is_active);

ALTER TABLE public.wellness_goals ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own goals"
    ON public.wellness_goals
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ============================================================
-- Daily Insights
-- AI-generated or heuristic wellness insights per user per day.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.daily_insights (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    summary TEXT NOT NULL DEFAULT '',
    highlights JSONB NOT NULL DEFAULT '[]',
    suggestions JSONB NOT NULL DEFAULT '[]',
    total_screen_time_minutes INTEGER NOT NULL DEFAULT 0,
    top_apps JSONB NOT NULL DEFAULT '[]',
    tier INTEGER NOT NULL DEFAULT 1, -- 1=heuristic, 2=on-device ML, 3=Anthropic API
    mood JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, date)
);

CREATE INDEX idx_insights_user_date ON public.daily_insights(user_id, date DESC);

ALTER TABLE public.daily_insights ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own insights"
    ON public.daily_insights FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Edge Functions can write insights"
    ON public.daily_insights FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Edge Functions can upsert insights"
    ON public.daily_insights FOR UPDATE
    USING (auth.uid() = user_id);

-- ============================================================
-- Push Tokens
-- Device tokens for FCM (Android) and APNs (iOS).
-- ============================================================
CREATE TABLE IF NOT EXISTS public.push_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    platform TEXT NOT NULL CHECK (platform IN ('android', 'ios')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, token)
);

ALTER TABLE public.push_tokens ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own push tokens"
    ON public.push_tokens
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ============================================================
-- Trigger: auto-update updated_at columns
-- ============================================================
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER trg_goals_updated_at
    BEFORE UPDATE ON public.wellness_goals
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- ============================================================
-- Trigger: auto-create profile on user sign-up
-- ============================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id)
    VALUES (NEW.id)
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trg_on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
