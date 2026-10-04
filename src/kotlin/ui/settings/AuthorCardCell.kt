package desu.inugram.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.UserObject
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.AvatarDrawable
import org.telegram.ui.Components.BackupImageView
import org.telegram.ui.Components.LayoutHelper
import java.util.Locale

@SuppressLint("ViewConstructor")
class AuthorCardCell(
    context: Context,
    private val resourcesProvider: Theme.ResourcesProvider?,
    private val username: String,
    githubUsername: String,
    onOpenTelegram: () -> Unit,
    onOpenGithub: () -> Unit,
) : FrameLayout(context), NotificationCenter.NotificationCenterDelegate {

    private val brand = 0xFFFA456C.toInt()

    private var account = -1
    private var userId = 0L
    private var requested = false

    private val avatarView = BackupImageView(context).apply {
        setRoundRadius(AndroidUtilities.dp(30f))
    }

    private val ringView = FrameLayout(context).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0)
            setStroke(AndroidUtilities.dp(2f), brand)
        }
        addView(avatarView, LayoutHelper.createFrame(60, 60, Gravity.CENTER))
    }

    private val chipView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f)
        setTypeface(AndroidUtilities.bold())
        setTextColor(brand)
        letterSpacing = 0.12f
        maxLines = 1
        text = LocaleController.getString(R.string.InuAboutAuthor).uppercase(Locale.getDefault())
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = AndroidUtilities.dp(8f).toFloat()
            setColor(ColorUtils.setAlphaComponent(brand, 0x24))
        }
        setPadding(
            AndroidUtilities.dp(8f),
            AndroidUtilities.dp(2f),
            AndroidUtilities.dp(8f),
            AndroidUtilities.dp(2f),
        )
    }

    private val nameView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)
        setTypeface(AndroidUtilities.bold())
        setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider))
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        text = username
    }

    private fun mkLinkChip(iconRes: Int, label: String, description: String, color: Int, onClick: () -> Unit): LinearLayout {
        val radius = AndroidUtilities.dp(17f)
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = Theme.createSimpleSelectorRoundRectDrawable(
                radius,
                color,
                ColorUtils.blendARGB(color, Color.WHITE, 0.18f),
            )
            setPadding(AndroidUtilities.dp(12f), 0, AndroidUtilities.dp(12f), 0)
            isClickable = true
            isFocusable = true
            contentDescription = description
            setOnClickListener { onClick() }
            addView(
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setImageResource(iconRes)
                    setColorFilter(Color.WHITE)
                },
                LayoutHelper.createLinear(18, 18),
            )
            addView(
                TextView(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
                    setTypeface(AndroidUtilities.bold())
                    setTextColor(Color.WHITE)
                    includeFontPadding = false
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    text = label
                },
                LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_VERTICAL, 6, 0, 0, 0),
            )
        }
    }

    private val telegramChip = mkLinkChip(
        R.drawable.inu_tabler_brand_telegram,
        "Telegram",
        "Telegram @$username",
        0xFF229ED9.toInt(),
        onOpenTelegram,
    )

    private val githubChip = mkLinkChip(
        R.drawable.inu_tabler_brand_github,
        "GitHub",
        "GitHub @$githubUsername",
        0xFF0D1117.toInt(),
        onOpenGithub,
    )

    private val card = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(AndroidUtilities.dp(16f), AndroidUtilities.dp(16f), AndroidUtilities.dp(16f), AndroidUtilities.dp(16f))

        val top = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ringView, LayoutHelper.createLinear(68, 68))
            val texts = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(chipView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
                addView(
                    nameView,
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 6f, 0f, 0f),
                )
            }
            addView(
                texts,
                LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL, 14, 0, 0, 0),
            )
        }
        addView(top, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 68))

        val links = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(telegramChip, LayoutHelper.createLinear(0, 34, 1f))
            addView(githubChip, LayoutHelper.createLinear(0, 34, 1f, Gravity.NO_GRAVITY, 8, 0, 0, 0))
        }
        addView(links, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 34, 0f, 12f, 0f, 0f))
    }

    init {
        addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER))
        showPlaceholder()
    }

    private fun showPlaceholder() {
        avatarView.setImageDrawable(
            AvatarDrawable().apply {
                setInfo(0L, username, null)
                setColor(brand)
            }
        )
    }

    private fun bind() {
        if (account < 0 || userId == 0L) return
        val user = MessagesController.getInstance(account).getUser(userId) ?: return
        avatarView.imageReceiver.currentAccount = account
        avatarView.setForUserOrChat(user, AvatarDrawable().apply { setInfo(user) })
        val name = UserObject.getUserName(user)
        if (name.isNotBlank()) nameView.text = name
    }

    private fun load() {
        if (requested) return
        val acc = UserConfig.selectedAccount
        if (!UserConfig.getInstance(acc).isClientActivated) return
        requested = true
        account = acc
        MessagesController.getInstance(acc).userNameResolver.resolve(username, null, true) { peerId ->
            if (peerId != null && peerId > 0L && peerId != Long.MAX_VALUE) {
                userId = peerId
                bind()
            } else {
                requested = false
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(146f), MeasureSpec.EXACTLY),
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        for (i in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            NotificationCenter.getInstance(i).addObserver(this, NotificationCenter.updateInterfaces)
        }
        load()
        bind()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        for (i in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            NotificationCenter.getInstance(i).removeObserver(this, NotificationCenter.updateInterfaces)
        }
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any) {
        if (id != NotificationCenter.updateInterfaces || account != this.account) return
        val mask = args[0] as? Int ?: return
        if (mask and (MessagesController.UPDATE_MASK_AVATAR or MessagesController.UPDATE_MASK_NAME) != 0) {
            bind()
        }
    }
}
