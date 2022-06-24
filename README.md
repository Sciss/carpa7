# carpa7

This repository contains materials for a colloquium. See
[Research Catalogue](https://www.researchcatalogue.net/view/1172657/1172658). In continuation, it is also
code used to prepare a video for xCoAx 2022.

(C)opyright 2021–2022 by Hanns Holger Rutz. All rights reserved. The code in this repository is released under the
[GNU Affero General Public License](https://codeberg.org/sciss/carpa7/blob/main/LICENSE) v3+ and
comes with absolutely no warranties.
To contact the author, send an e-mail to `contact at sciss.de`. The artifacts and workspaces in this
repository are licensed under [CC BY-NC-ND 4.0](https://creativecommons.org/licenses/by-nc-nd/4.0/).

## install

You need G'MIC

    sudo apt install gmic

## video encoding

### Kontakt

    ffmpeg -i '/data/projects/Kontakt/image_transfer_rsmp/transfer-rsmp-%d.jpg' -r 25 '/data/projects/Kontakt/materials/transfer-rsmp.mp4'

    ffmpeg -i '/data/projects/Kontakt/image_transfer_rsmp/transfer-rsmp-%d.jpg' -r 25 -filter:v "crop=1080:1080:180:180,fade=type=out:start_frame=2225:nb_frames=25" '/data/projects/Kontakt/materials/transfer-rsmp-cr.mp4'

### Unlike

    ffmpeg -i '/data/projects/Unlike/image_transfer_rsmp/transfer-rsmp-%d.jpg' -r 25 -filter:v "crop=3484:1960:118:284,scale=1920:1080,fade=type=out:start_frame=2222:nb_frames=25" '/data/projects/Unlike/materials/transfer-rsmp-cr.mp4'

### Hybrid

    ffmpeg -i '/data/projects/BookOfX/image_transfer_rsmp/transfer-rsmp-%d.jpg' -r 25 -filter:v "scale=761:1080,fade=type=out:start_frame=2225:nb_frames=25" '/data/projects/BookOfX/materials/transfer-rsmp-cr.mp4'
