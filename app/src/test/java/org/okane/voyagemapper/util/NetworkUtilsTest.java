package org.okane.voyagemapper.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NetworkUtilsTest {

    @Test
    void returnsFalseWhenConnectivityManagerIsNull() {
        Context ctx = mock(Context.class);
        when(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null);

        assertFalse(NetworkUtils.isNetworkAvailable(ctx));

        verify(ctx).getSystemService(Context.CONNECTIVITY_SERVICE);
        verifyNoMoreInteractions(ctx);
    }

    @Test
    void returnsFalseWhenActiveNetworkIsNull() {
        Context ctx = mock(Context.class);
        ConnectivityManager cm = mock(ConnectivityManager.class);

        when(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(cm);
        when(cm.getActiveNetwork()).thenReturn(null);

        assertFalse(NetworkUtils.isNetworkAvailable(ctx));

        verify(ctx).getSystemService(Context.CONNECTIVITY_SERVICE);
        verify(cm).getActiveNetwork();
        verifyNoMoreInteractions(cm);
        verifyNoMoreInteractions(ctx);
    }

    @Test
    void returnsFalseWhenNetworkCapabilitiesIsNull() {
        Context ctx = mock(Context.class);
        ConnectivityManager cm = mock(ConnectivityManager.class);
        Network network = mock(Network.class);

        when(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(cm);
        when(cm.getActiveNetwork()).thenReturn(network);
        when(cm.getNetworkCapabilities(network)).thenReturn(null);

        assertFalse(NetworkUtils.isNetworkAvailable(ctx));

        verify(cm).getActiveNetwork();
        verify(cm).getNetworkCapabilities(network);
        verifyNoMoreInteractions(ctx);
        verifyNoMoreInteractions(network);
        verifyNoMoreInteractions(cm);
    }

    @Test
    void returnsFalseWhenInternetCapabilityMissing() {
        Context ctx = mock(Context.class);
        ConnectivityManager cm = mock(ConnectivityManager.class);
        Network network = mock(Network.class);
        NetworkCapabilities caps = mock(NetworkCapabilities.class);

        when(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(cm);
        when(cm.getActiveNetwork()).thenReturn(network);
        when(cm.getNetworkCapabilities(network)).thenReturn(caps);

        // internet=false, validated=true
        when(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(false);

        assertFalse(NetworkUtils.isNetworkAvailable(ctx));

        verify(caps).hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        verifyNoMoreInteractions(ctx);
        verifyNoMoreInteractions(cm);
        verifyNoMoreInteractions(network);
        verifyNoMoreInteractions(caps);
    }

    @Test
    void returnsFalseWhenValidatedCapabilityMissing() {
        Context ctx = mock(Context.class);
        ConnectivityManager cm = mock(ConnectivityManager.class);
        Network network = mock(Network.class);
        NetworkCapabilities caps = mock(NetworkCapabilities.class);

        when(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(cm);
        when(cm.getActiveNetwork()).thenReturn(network);
        when(cm.getNetworkCapabilities(network)).thenReturn(caps);

        // internet=true, validated=false
        when(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(true);
        when(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)).thenReturn(false);

        assertFalse(NetworkUtils.isNetworkAvailable(ctx));

        verify(caps).hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        verify(caps).hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        verifyNoMoreInteractions(ctx);
        verifyNoMoreInteractions(cm);
        verifyNoMoreInteractions(network);
        verifyNoMoreInteractions(caps);
    }

    @Test
    void returnsTrueWhenInternetAndValidatedPresent() {
        Context ctx = mock(Context.class);
        ConnectivityManager cm = mock(ConnectivityManager.class);
        Network network = mock(Network.class);
        NetworkCapabilities caps = mock(NetworkCapabilities.class);

        when(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(cm);
        when(cm.getActiveNetwork()).thenReturn(network);
        when(cm.getNetworkCapabilities(network)).thenReturn(caps);

        when(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(true);
        when(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)).thenReturn(true);

        assertTrue(NetworkUtils.isNetworkAvailable(ctx));

        verify(caps).hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        verify(caps).hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        verifyNoMoreInteractions(ctx);
        verifyNoMoreInteractions(cm);
        verifyNoMoreInteractions(network);
        verifyNoMoreInteractions(caps);
    }
}
