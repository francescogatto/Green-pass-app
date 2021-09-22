package it.ministerodellasalute.verificaC19.ui.custom

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.vansuita.materialabout.builder.AboutBuilder
import it.ministerodellasalute.verificaC19.R
import it.ministerodellasalute.verificaC19.databinding.FInfoBinding

class AboutMeFragment : Fragment() {

    private var _binding: FInfoBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    lateinit var dialog: AlertDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        val canvas = Canvas(bmp);
        canvas.drawColor(ContextCompat.getColor(requireContext(), R.color.white))
        val builder = AboutBuilder.with(requireContext())
            .setAppIcon(R.mipmap.ic_launcher)
            .setAppName(R.string.app_name)
            .setCover(bmp)
            .setLinksAnimated(true)
            .setDividerDashGap(13)
            .setPhoto(R.drawable.ic_logo)
            .setName("Green Pass App")
            .setSubTitle("La migliore app per il Green Pass, Open Source e senza pubblicità")
            .addGitHubLink("francescogatto/Green-pass-app")
            .setLinksColumnsCount(4)
            // .setBrief(
            //     "App non ufficiale della Lotteria degli Scontrini \n" +
            //             "Some vectors created by freepik - www.freepik.com"
            // )
            .addPrivacyPolicyAction {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.iubenda.com/privacy-policy/41679866")
                    )
                )
            }
            .addFiveStarsAction()
            .setVersionNameAsAppSubTitle()
            .addShareAction(R.string.app_name)
            .addUpdateAction()
            .setActionsColumnsCount(2)
            .addFeedbackAction("francescogatto@live.it")
            // .addPrivacyPolicyAction("http://www.docracy.com/2d0kis6uc2")
            //.addIntroduceAction(null as Intent?)
            //.addHelpAction(null as Intent?)
            //.addChangeLogAction(null as Intent?)
            //.addRemoveAdsAction(null as Intent?)
            .setWrapScrollView(true)
            .setShowAsCard(true)
        binding.aboutMe.addView(builder.build())

    }
}
