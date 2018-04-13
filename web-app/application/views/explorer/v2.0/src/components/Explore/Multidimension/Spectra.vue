<template>
    <div>
        <h5>Spectra</h5>
        <div :id="'spectra-'+currentMap.id"></div>
    </div>
</template>

<script>
import Plotly from 'plotly.js/lib/core';

export default {
    name: 'Spectra',
    props: [
        'currentMap',
        'imageSequence',
        'imageGroup',
    ],
    data() {
        return {
            spectra: {},
            hdf5: {},
            yAxis: [],
        }
    },
    computed: {
        xAxis() {
            let xAxis = [];
            this.imageGroup.map((image, index) => xAxis.push(index + 1))
            return xAxis;
        }
    },
    methods: {
        updateSpectra(newData = this.yAxis) {
            let trace = {
                y: newData,
                type: 'scatter',
            };
            let layout = {
                title: 'Spectral distribution',
                xaxis: {
                    range: [0, this.imageGroup.length],
                }
            }
            Plotly.newPlot('spectra-'+this.currentMap.id, [trace], layout)
        },
        getPixelData(event) {
            api.get(`/api/imagegroupHDF5/${this.hdf5.id}/${Math.round(event.pixel[0])}/${Math.round(event.pixel[1])}/pixel.json`).then(data => {
                this.yAxis = data.data.spectra;
                this.updateSpectra();
            })
        }
    },
    created() {
        api.get(`/api/imagegroup/${this.currentMap.imageGroup}/imagegroupHDF5.json`).then(data => {
            this.hdf5 = data.data;
            this.$openlayers.getMap(this.currentMap.id).on('click', this.getPixelData)
        })
    },
}
</script>