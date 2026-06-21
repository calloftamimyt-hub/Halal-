package com.example

import io.github.jan_tennert.supabase.auth.Auth
import io.github.jan_tennert.supabase.createSupabaseClient
import io.github.jan_tennert.supabase.postgrest.Postgrest

object Supabase {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}
