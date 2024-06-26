package au.com.shiftyjelly.pocketcasts.player.view

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentSleepBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.utils.combineLatest
import au.com.shiftyjelly.pocketcasts.utils.minutes
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class SleepFragment : BaseDialogFragment() {

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    override val statusBarColor: StatusBarColor? = null

    private val viewModel: PlayerViewModel by activityViewModels()
    private var binding: FragmentSleepBinding? = null
    private var disposable: Disposable? = null

    override fun onResume() {
        super.onResume()

        // refresh the sleep time every second
        disposable = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { Timber.e(it) },
                onNext = { viewModel.updateSleepTimer() }
            )
    }

    override fun onPause() {
        super.onPause()

        disposable?.dispose()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSleepBinding.inflate(inflater, container, false)
        this.binding = binding

        binding.buttonMins5.setOnClickListener { startTimer(mins = 5) }
        binding.buttonMins15.setOnClickListener { startTimer(mins = 15) }
        binding.buttonMins30.setOnClickListener { startTimer(mins = 30) }
        binding.buttonMins60.setOnClickListener { startTimer(mins = 60) }
        binding.buttonEndOfEpisode.setOnClickListener {
            analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_ENABLED, mapOf(TIME_KEY to END_OF_EPISODE))
            startTimerEndOfEpisode()
        }
        binding.buttonAfterNEpisodes.setOnClickListener { startTimerByEpisode() }
        binding.incEpisodeButton.setOnClickListener { addEpisodeForTimer() }
        binding.decEpisodeButton.setOnClickListener { subtractEpisodeForTimer() }
        if (viewModel.isMinEpisodeLimit()) {
            binding.decEpisodeButton.alpha = .5f
        } else if (viewModel.isMaxEpisodeLimit()) {
            binding.incEpisodeButton.alpha = .5f
        }
        binding.customMinusButton.setOnClickListener { minusButtonClicked() }
        binding.customPlusButton.setOnClickListener { plusButtonClicked() }
        binding.buttonCustom.setOnClickListener { startCustomTimer() }
        binding.buttonAdd5Minute.setOnClickListener { addExtra5minute() }
        binding.buttonAdd1Minute.setOnClickListener { addExtra1minute() }
        binding.buttonEndOfEpisode2.setOnClickListener {
            analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_EXTENDED, mapOf(AMOUNT_KEY to END_OF_EPISODE))
            startTimerEndOfEpisode()
        }
        binding.buttonAddEpisode.setOnClickListener { addEpisodeToTimer(eps = 1) }
        binding.buttonAdd2Episodes.setOnClickListener { addEpisodeToTimer(eps = 2) }
        binding.buttonAddNEpisodes.setOnClickListener { addEpisodeToTimer(eps = null) }
        binding.buttonEndOfCurrentEpisode.setOnClickListener {
            cancelEpisodeTimer(false)
            startTimerEndOfEpisode()
        }
        binding.buttonCancelEpisodeTimer.setOnClickListener { cancelEpisodeTimer() }
        binding.buttonCancelTime.setOnClickListener { cancelTimer() }
        binding.buttonCancelEndOfEpisode.setOnClickListener { cancelTimer() }

        return binding.root
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.sleepTimeLeftText.observe(viewLifecycleOwner) { sleepTime ->
            binding?.sleepTime?.text = sleepTime
        }

        viewModel.sleepCustomTimeText.observe(viewLifecycleOwner) { customTimeText ->
            binding?.labelCustom?.text = customTimeText
        }

        viewModel.episodesUntilSleepText.observe(viewLifecycleOwner) { episodesText ->
            binding?.labelAfterNEpisodes?.text = episodesText
        }

        viewModel.episodesLeftUntilSleepText.observe(viewLifecycleOwner) { episodesLeftText ->
            binding?.episodesLeft?.text = episodesLeftText
        }

        viewModel.customEpisodeIncrementText.observe(viewLifecycleOwner) { customEpisodeIncText ->
            binding?.buttonAddNEpisodes?.text = customEpisodeIncText
        }

        viewModel.isSleepRunning.observe(viewLifecycleOwner) { isSleepRunning ->
            binding?.sleepSetup?.isVisible = !isSleepRunning
            binding?.sleepRunning?.isVisible = isSleepRunning
        }

        viewModel.isSleepRunning.combineLatest(viewModel.isSleepAtEndOfEpisode).combineLatest(viewModel.isSleepForEpisodes)
            .observe(viewLifecycleOwner) { (sleepChecksPair, isSleepForEpsisodes) ->
                val (isSleepRunning, isSleepAtEndOfEpisode) = sleepChecksPair
                binding?.sleepRunningTime?.isVisible = isSleepRunning && !isSleepAtEndOfEpisode && !isSleepForEpsisodes
                binding?.sleepRunningEndOfEpisode?.isVisible = isSleepRunning && isSleepAtEndOfEpisode && !isSleepForEpsisodes
                binding?.sleepRunningByEpisode?.isVisible = isSleepRunning && !isSleepAtEndOfEpisode && isSleepForEpsisodes
            }

        viewModel.playingEpisodeLive.observe(
            viewLifecycleOwner,
            Observer { (_, backgroundColor) ->
                applyColor(theme, backgroundColor)

                val tintColor = theme.playerHighlightColor(viewModel.podcast)
                val tintColorStateList = ColorStateList.valueOf(tintColor)
                val binding = binding ?: return@Observer
                binding.buttonAdd5Minute.strokeColor = tintColorStateList
                binding.buttonAdd5Minute.setTextColor(tintColorStateList)
                binding.buttonAdd1Minute.strokeColor = tintColorStateList
                binding.buttonAdd1Minute.setTextColor(tintColorStateList)
                binding.buttonCancelEndOfEpisode.strokeColor = tintColorStateList
                binding.buttonCancelEndOfEpisode.setTextColor(tintColorStateList)
                binding.buttonCancelTime.strokeColor = tintColorStateList
                binding.buttonCancelTime.setTextColor(tintColorStateList)
                binding.buttonEndOfEpisode2.strokeColor = tintColorStateList
                binding.buttonEndOfEpisode2.setTextColor(tintColorStateList)
                binding.buttonAddEpisode.strokeColor = tintColorStateList
                binding.buttonAddEpisode.setTextColor(tintColorStateList)
                binding.buttonAdd2Episodes.strokeColor = tintColorStateList
                binding.buttonAdd2Episodes.setTextColor(tintColorStateList)
                binding.buttonAddNEpisodes.strokeColor = tintColorStateList
                binding.buttonAddNEpisodes.setTextColor(tintColorStateList)
                binding.buttonEndOfCurrentEpisode.strokeColor = tintColorStateList
                binding.buttonEndOfCurrentEpisode.setTextColor(tintColorStateList)
                binding.buttonCancelEpisodeTimer.strokeColor = tintColorStateList
                binding.buttonCancelEpisodeTimer.setTextColor(tintColorStateList)

                binding.sleepAnimation.post { // this only works the second time it's called unless it's in a post
                    binding.sleepAnimation.addValueCallback(KeyPath("**"), LottieProperty.COLOR) { tintColor }
                }
            }
        )

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun addExtra5minute() {
        viewModel.sleepTimerAddExtraMins(mins = 5)
        analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_EXTENDED, mapOf(AMOUNT_KEY to TimeUnit.MILLISECONDS.toSeconds(5.minutes())))
        viewModel.timeLeftInSeconds()?.let { timeLeft ->
            binding?.root?.announceForAccessibility("5 minutes added to sleep timer. ${timeLeft / 60} minutes ${timeLeft % 60} seconds remaining")
        }
    }

    private fun addExtra1minute() {
        viewModel.sleepTimerAddExtraMins(mins = 1)
        analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_EXTENDED, mapOf(AMOUNT_KEY to TimeUnit.MILLISECONDS.toSeconds(1.minutes())))
        viewModel.timeLeftInSeconds()?.let { timeLeft ->
            binding?.root?.announceForAccessibility("1 minute added to sleep timer. ${timeLeft / 60} minutes ${timeLeft % 60} seconds remaining")
        }
    }

    private fun startCustomTimer() {
        viewModel.sleepTimerAfter(mins = viewModel.sleepCustomTimeMins)
        binding?.root?.announceForAccessibility("Sleep timer set for ${viewModel.sleepCustomTimeMins} minutes")
        analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_ENABLED, mapOf(TIME_KEY to TimeUnit.MILLISECONDS.toSeconds(viewModel.sleepCustomTimeMins.minutes())))
        close()
    }

    private fun plusButtonClicked() {
        if (viewModel.sleepCustomTimeMins < 5) {
            viewModel.sleepCustomTimeMins += 1
        } else {
            viewModel.sleepCustomTimeMins += 5
        }
        binding?.root?.announceForAccessibility("Custom sleep time ${viewModel.sleepCustomTimeMins}")
    }

    private fun minusButtonClicked() {
        if (viewModel.sleepCustomTimeMins <= 5) {
            viewModel.sleepCustomTimeMins -= 1
        } else {
            viewModel.sleepCustomTimeMins -= 5
        }
        binding?.root?.announceForAccessibility("Custom sleep time ${viewModel.sleepCustomTimeMins}")
    }

    private fun startTimerEndOfEpisode() {
        viewModel.sleepTimerAfterEpisode()
        binding?.root?.announceForAccessibility("Sleep timer set for end of episode")
        close()
    }

    private fun startTimer(mins: Int) {
        viewModel.sleepTimerAfter(mins = mins)
        binding?.root?.announceForAccessibility("Sleep timer set for $mins minutes")
        analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_ENABLED, mapOf(TIME_KEY to TimeUnit.MILLISECONDS.toSeconds(mins.minutes())))
        close()
    }

    private fun startTimerByEpisode() {
        viewModel.sleepTimerByEpisode()
        viewModel.episodeSleepCount().let { eps ->
            binding?.root?.announceForAccessibility("Sleep timer set for $eps episode" + if (eps != 1) "s" else "")
            analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_ENABLED, mapOf(EPISODE_KEY to eps))
        }
        close()
    }

    private fun addEpisodeForTimer() {
        viewModel.incEpisodesForTimer()
        if (viewModel.isMaxEpisodeLimit()) {
            binding?.incEpisodeButton?.alpha = .5f
        } else if (!viewModel.isMinEpisodeLimit()) {
            binding?.decEpisodeButton?.alpha = 1f
        }
    }

    private fun subtractEpisodeForTimer() {
        viewModel.decEpisodesForTimer()
        if (viewModel.isMinEpisodeLimit()) {
            binding?.decEpisodeButton?.alpha = .5f
        } else if (!viewModel.isMaxEpisodeLimit()) {
            binding?.incEpisodeButton?.alpha = 1f
        }
    }

    private fun addEpisodeToTimer(eps: Int?) {
        viewModel.extendEpisodeTimer(eps)
        eps.let {
            analyticsTracker.track(
                AnalyticsEvent.PLAYER_SLEEP_TIMER_EXTENDED,
                if (it == null) mapOf(AMOUNT_KEY to viewModel.episodeSleepCount()) else mapOf(AMOUNT_KEY to it)
            )
        }
    }

    private fun cancelTimer() {
        viewModel.cancelSleepTimer()
        binding?.root?.announceForAccessibility("Sleep timer cancelled")
        analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_CANCELLED)
        close()
    }

    private fun cancelEpisodeTimer(dismiss: Boolean = true) {
        viewModel.cancelEpisodeTimer()
        if (dismiss) {
            binding?.root?.announceForAccessibility("Sleep timer cancelled")
            analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_CANCELLED)
            close()
        }
    }

    private fun close() {
        dismiss()
    }

    companion object {
        private const val TIME_KEY = "time" // in seconds
        private const val AMOUNT_KEY = "amount"
        private const val END_OF_EPISODE = "end_of_episode"
        private const val EPISODE_KEY = "episode"
    }
}
