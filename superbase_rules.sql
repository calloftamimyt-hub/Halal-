-- ==========================================
-- SUPABASE SQL RULES & TABLES FOR APPLICATION
-- ==========================================
-- Copy all of this code and paste it into the "SQL Editor" in your Supabase Dashboard, then click "Run".

-- 1. Create table for Social Posts ("What's on your mind?")
CREATE TABLE IF NOT EXISTS public.posts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    user_name TEXT,
    avatar_url TEXT,
    text_content TEXT,
    media_url TEXT,
    media_type TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. Create table for Parental Control Pairing Codes (If using Supabase for this)
CREATE TABLE IF NOT EXISTS public.pairing_codes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    child_uid TEXT,
    child_name TEXT,
    pin_code TEXT,
    timestamp BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ==========================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==========================================

-- Enable RLS on tables
ALTER TABLE public.posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pairing_codes ENABLE ROW LEVEL SECURITY;

-- Posts Policies
-- Anyone can see all posts
CREATE POLICY "Enable read access for all users" ON public.posts
    FOR SELECT USING (true);

-- Authenticated users can insert their own posts
CREATE POLICY "Enable insert for authenticated users only" ON public.posts
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Pairing Codes Policies
CREATE POLICY "Enable read access for all" ON public.pairing_codes
    FOR SELECT USING (true);

CREATE POLICY "Enable insert access for all" ON public.pairing_codes
    FOR INSERT WITH CHECK (true);

-- ==========================================
-- REALTIME CONFIGURATION
-- ==========================================
-- This enables real-time updates so that new posts appear instantly on the screen without refreshing!

ALTER PUBLICATION supabase_realtime ADD TABLE public.posts;
ALTER PUBLICATION supabase_realtime ADD TABLE public.pairing_codes;
