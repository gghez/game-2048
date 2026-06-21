package com.gghez.game2048.data.leaderboard

import android.app.Activity
import com.google.android.gms.games.PlayGames
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

/**
 * Real Google Play Games leaderboard implementation (Play Games Services v2).
 *
 * Only constructed when valid game ids are configured (see [LeaderboardProvider]).
 * The v2 clients require an [Activity]; the host binds it through [attach] and we
 * hold it weakly to avoid leaks. Calls made while no Activity is attached are
 * skipped silently.
 */
class PlayGamesLeaderboard(
    private val speedId: String,
    private val efficiencyId: String,
) : LeaderboardRepository {

    private var activityRef: WeakReference<Activity> = WeakReference(null)
    private val activity: Activity? get() = activityRef.get()

    override val isAvailable: Boolean = speedId.isNotEmpty() && efficiencyId.isNotEmpty()

    override fun attach(activity: Activity?) {
        activityRef = WeakReference(activity)
    }

    override suspend fun signInSilently(): Boolean = suspendCancellableCoroutine { cont ->
        val act = activity
        if (act == null) {
            cont.resume(false)
            return@suspendCancellableCoroutine
        }
        runCatching {
            PlayGames.getGamesSignInClient(act).isAuthenticated
                .addOnCompleteListener { task ->
                    cont.resume(task.isSuccessful && task.result.isAuthenticated)
                }
        }.onFailure { cont.resume(false) }
    }

    override suspend fun submit(kind: LeaderboardKind, value: Long) {
        if (!isAvailable) return
        val act = activity ?: return
        val id = when (kind) {
            LeaderboardKind.SPEED -> speedId
            LeaderboardKind.EFFICIENCY -> efficiencyId
        }
        runCatching { PlayGames.getLeaderboardsClient(act).submitScore(id, value) }
    }

    override fun showLeaderboards(activity: Activity) {
        if (!isAvailable) return
        runCatching {
            PlayGames.getLeaderboardsClient(activity).allLeaderboardsIntent
                .addOnSuccessListener { intent -> activity.startActivityForResult(intent, RC_LEADERBOARD) }
        }
    }

    companion object {
        private const val RC_LEADERBOARD = 9004
    }
}
