import ClayButton from 'clay-button';
import Component from 'metal-jsx';
import Notifications from '../../util/Notifications.es';
import {Config} from 'metal-state';

class PreviewButton extends Component {
	static PROPS = {
		resolvePreviewURL: Config.func().required(),
		spritemap: Config.string().required()
	};

	preview() {
		const {resolvePreviewURL} = this.props;

		return resolvePreviewURL()
			.then(
				previewURL => {
					window.open(previewURL, '_blank');

					return previewURL;
				}
			).catch(
				() => {
					Notifications.showError(Liferay.Language.get('your-request-failed-to-complete'));
				}
			);
	}

	render() {
		const {spritemap} = this.props;

		return (
			<ClayButton
				elementClasses={'btn-default'}
				events={
					{
						click: this._handleButtonClicked.bind(this)
					}
				}
				label={Liferay.Language.get('preview-form')}
				ref={'button'}
				spritemap={spritemap}
				style={'link'}
			/>
		);
	}

	_handleButtonClicked() {
		this.preview();
	}
}

export default PreviewButton;