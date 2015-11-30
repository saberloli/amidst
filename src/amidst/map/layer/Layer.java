package amidst.map.layer;

import amidst.preferences.PrefModel;

public class Layer {
	private final LayerType layerType;
	private final PrefModel<Boolean> isVisiblePreference;
	private final FragmentDrawer drawer;
	private final FragmentLoader loader;

	public Layer(LayerType layerType, PrefModel<Boolean> isVisiblePreference,
			FragmentDrawer drawer, FragmentLoader loader) {
		this.layerType = layerType;
		this.isVisiblePreference = isVisiblePreference;
		this.drawer = drawer;
		this.loader = loader;
	}

	public LayerType getLayerType() {
		return layerType;
	}

	public boolean isVisible() {
		return isVisiblePreference.get();
	}

	public FragmentDrawer getFragmentDrawer() {
		return drawer;
	}

	public FragmentLoader getFragmentLoader() {
		return loader;
	}
}
