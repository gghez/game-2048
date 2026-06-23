package com.gghez.game2048.data.leaderboard

/**
 * SPEED        = best score (mockup: "Vitesse d'exécution").
 * EFFICIENCY   = best tile reached (mockup: "Optimisation des déplacements").
 * TIME_TO_2048 = fastest time to reach the 2048 tile, submitted in milliseconds.
 *                Smaller is better — the Play Console board must use a "Time"
 *                score type ordered low-to-high.
 */
enum class LeaderboardKind { SPEED, EFFICIENCY, TIME_TO_2048 }
