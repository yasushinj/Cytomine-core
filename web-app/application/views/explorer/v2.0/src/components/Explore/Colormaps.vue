<template>
  <div>
    <div class="alert alert-info text-center">Click on the graph to select a value to edit</div>
    <div :id="'colormaps-' + currentMap.id"></div>
    <div class="btn-group flex">
      <button class="btn btn-default" v-if="modeSelected" @click="switchMode">{{modeToShow}}</button>
      <select :class="`btn btn-${colorToRender}`" v-if="modeSelected == 'rgb'" v-model="colorSelected">
        <option value="r">Red</option>
        <option value="g">Green</option>
        <option value="b">Blue</option>
      </select>
    </div>
    <div class="flex mt-4">
      <div class="input-group">
        <label class="input-group-addon" for="">Click tolerance</label>
        <input class="form-control" v-model.number="clickTolerance" type="number" step="1" :max="(Math.pow(2, data.bitdepth) - 1) / 10" :min="0">
      </div>
      <input class="range" v-model.number="clickTolerance" type="range" step="1" :max="(Math.pow(2, data.bitdepth) - 1) / 10" :min="0">
    </div>
    <div class="flex mt-4">
      <div class="input-group">
        <label class="input-group-addon" for="">x:</label>
        <input class="form-control" v-model.number="xSelected" type="number" step="1" :max="Math.pow(2, data.bitdepth) - 1" :min="0">
      </div>
      <input class="range" v-model.number="xSelected" type="range" step="1" :max="Math.pow(2, data.bitdepth) - 1" :min="0">
    </div>

    <div class="flex mt-4">
      <div class="input-group">
        <label class="input-group-addon" for="">y:</label>
        <input class="form-control" v-model.number="yValue" type="number" step="1" max="255" min="0">
      </div>
      <input class="range" v-model.number="yValue" type="range" step="1" max="255" min="0">
    </div>
  </div>
</template>

<script>
import Plotly from 'plotly.js/lib/core';

export default {
    name: 'Colormaps',
    props: [
      'currentMap'
    ],
    data() {
      return {
        data: {
          bitdepth: 16,
          colorspace: 'rgb',
        },
        colorSelected: 'r',
        modeSelected: 'grayscale',
        xSelected: 0,
        yValue: 0,
        traces: {
          r: {},
          g: {},
          b: {},
          l: {},
        },
        layout: {},
        clickTolerance: 0,
      }
    },
    computed: {
      graphDiv() {
        return document.getElementById(this.colormapId);
      },
      colormapId() {
        return `colormaps-${this.currentMap.id}`;
      },
      tracesArray() {
        return this.modeSelected == 'rgb' ? [this.traces.r, this.traces.g, this.traces.b] : [this.traces.l];
      },
      modeToShow() {
        if(this.data.colorspace == 'grayscale') {
          return false;
        } else if(this.data.colorspace == 'rgb' && this.modeSelected == 'grayscale') {
          return 'RGB'
        } else {
          return 'Grayscale'
        }
      },
      colorToRender() {
        switch (this.colorSelected) {
          case 'r':
            return 'danger'
            break;
          case 'g':
            return 'success'
            break;
          case 'b':
            return 'info'
            break;
        
          default:
            return 'default'
            break;
        }
      }
    },
    watch: {
      yValue() {
        let color = this.modeSelected == 'rgb' ? this.colorSelected : 'l'
        let trace = this.traces[color];
        let index = () => trace.x.findIndex(item => item == this.xSelected);

        if(index() < 0) {
          // Adds new x to the trace
          trace.x.push(this.xSelected);
          // Sorts the array to place the new x a the right place
          trace.x = trace.x.sort((a, b) => {
              return a - b;
          });
          // Put the new y to the same index as new x
          trace.y.splice(index(), 0, this.yValue);
        } else {
          trace.y[index()] = this.yValue;
        }

        this.traces[color] = trace;
        this.layout.datarevision++;

        Plotly.update(this.colormapId, this.tracesArray, this.layout)
      }
    },
    methods: {
      newTrace(color) {
        let lineColor;
        switch (color) {
          case 'red':
            lineColor = 'rgb(255, 0, 0)';
            break;
          case 'green':
            lineColor = 'rgb(0, 255, 0)';
            break;
          case 'blue':
            lineColor = 'rgb(0, 0, 255)';
            break;
          case 'luminance':
            lineColor = 'rgb(0, 0, 0)';
            break;
        }
        return {
          x: this.data.bitdepth ? [0, Math.pow(2, this.data.bitdepth) - 1] : [0, Math.pow(2, 8) - 1],
          y: [0, 255],
          type: 'scatter',
          name: color[0].toUpperCase() + color.substr(1),
          line: {
            color: lineColor,
          }
        }
      },
      setValueToEdit(data) {
        let color = this.modeSelected == 'rgb' ? this.colorSelected : 'l'
        let trace = this.traces[color];
        let yCoordinate;
        // Uses p2l() method to determine coordinate from pixel postion
        let xCoordinate = Math.round(data.points[0].xaxis.p2l(data.event.layerX - data.points[0].xaxis._offset));
        let index = trace.x.findIndex(item => item == xCoordinate);
        let deltaX = data.points[0].x - xCoordinate;

        if(index > 0) {
          // If the point has already been created then get its y value
          yCoordinate = trace.y[index];
        } else if(deltaX < this.clickTolerance && deltaX > -this.clickTolerance) {
          // If at less than 700 from closest point then use closest point
          index = trace.x.findIndex(item => item == data.points[0].x);
          yCoordinate = trace.y[index];
          xCoordinate = trace.x[index];
        } else {
          // Get firstPoint for 1st degree function
          let firstPointIndex = trace.x.reverse().findIndex(item => item < xCoordinate);
          trace.x.reverse();
          firstPointIndex = (trace.x.length - 1) - firstPointIndex;
          let firstPoint = [trace.x[firstPointIndex], trace.y[firstPointIndex]];
          
          // Get secondPoint
          let secondPointIndex = trace.x.findIndex(item => item > xCoordinate);
          let secondPoint = [trace.x[secondPointIndex], trace.y[secondPointIndex]]

          let m = (secondPoint[1] - firstPoint[1])/(secondPoint[0] - firstPoint[0]);
          let b = firstPoint[1] - (m*firstPoint[0]); 

          yCoordinate = Math.round(m*xCoordinate+b);
        }

        this.yValue = yCoordinate;
        this.xSelected = xCoordinate;
      },
      switchMode() {
        this.modeSelected == 'rgb' ? this.modeSelected = 'grayscale' : this.modeSelected = 'rgb'
        this.layout.datarevision++;
        Plotly.react(this.colormapId, this.tracesArray, this.layout)
      },
    },
    mounted() {
      this.traces.r = this.newTrace('red');
      this.traces.g = this.newTrace('green');
      this.traces.b = this.newTrace('blue');
      this.traces.l = this.newTrace('luminance');

      this.layout = {
        title: 'Colormap',
        xaxis: {
          fixedrange: true,
        },
        yaxis: {
          fixedrange: true,
        },
        hovermode: 'closest',
        hoverdistance: -1,
        datarevision: 0,
      }

      Plotly.react(this.colormapId, this.tracesArray, this.layout, {displayModeBar: false});
      this.graphDiv.on('plotly_click', (data) => this.setValueToEdit(data));
      this.clickTolerance = Math.pow(2, this.data.bitdepth/2);
    }
}
</script>

<style scoped>
  .flex {
    display: flex;  
    max-width: 100%;
  }
  .input-group {
    max-width: 300px;
  }
  .range {
    width: auto;
  }
</style>
