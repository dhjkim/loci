package edu.cens.loci.components;

import android.location.Location;

oneway interface ILociListener
{
	void onLocationChanged(in Location location);
}