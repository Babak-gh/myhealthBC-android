package ca.bc.gov.bchealth.ui.login.error

import androidx.compose.runtime.Composable
import ca.bc.gov.bchealth.R
import ca.bc.gov.bchealth.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BcscAuthErrorFragment : BaseFragment(null) {

    @Composable
    override fun GetComposableLayout() {
        BcscAuthErrorUI(::popNavigation, ::onClickEmail)
    }

    private fun onClickEmail() {
        composeEmail(getString(R.string.bcsc_auth_error_click_email))
    }
}
