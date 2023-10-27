package io.horizontalsystems.bankwallet.owlwallet.countrypicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.core.findNavController

class CountryPickerFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                CountryPickerScreen(findNavController())
            }
        }
    }
}

@Composable
fun CountryPickerScreen(
    navController: NavController,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel<CountryPickerViewModel>(factory = CountryPickerModule.Factory())

    ComposeAppTheme {
        Scaffold(
            scaffoldState = scaffoldState,
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .padding(paddingValues)
                    .fillMaxHeight()
                    .background(color = ComposeAppTheme.colors.tyler)
            ) {
                AppBar(
                    title = stringResource(R.string.Binding_Kyc_Nationality),
                    navigationIcon = {
                        HsIconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.jacob
                            )
                        }
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))

                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = "",
                    hint = stringResource(id = R.string.Binding_Kyc_Nationality_Hint),
                    pasteEnabled = false,
                    singleLine = true,
                    state = null,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    onValueChange = {
                        viewModel.updateFilter(it)
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    stringResource(if (viewModel.filter.isEmpty()) R.string.Binding_Kyc_Nationality_All else R.string.Binding_Kyc_Nationality_Results),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W700,
                    color = ComposeAppTheme.colors.leah,
                )

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    items(viewModel.uiState.value.size) { index ->
                        val country = viewModel.uiState.value[index]

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable {
                                    viewModel.selectCountry(country)
                                    navController.popBackStack()
                                }
                                .background(color = ComposeAppTheme.colors.lawrence)
                        ) {
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(modifier = Modifier.width(16.dp))
                                    AsyncImage(
                                        model = country.flagUrl,
                                        contentDescription = "",
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))
                                    subhead2_leah(
                                        text = country.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                }
                            }
                            Divider(
                                thickness = 1.dp,
                                color = ComposeAppTheme.colors.steel10,
//                                modifier = Modifier.align(Alignment.Bottom)
                            )
                        }
                    }
                }
            }
        }
    }
}
